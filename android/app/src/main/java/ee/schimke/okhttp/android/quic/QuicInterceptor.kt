package ee.schimke.okhttp.android.quic

import android.content.Context
import android.util.Log
import com.babylon.certificatetransparency.certificateTransparencyHostnameVerifier
import com.google.android.gms.net.CronetProviderInstaller
import com.google.android.gms.tasks.Task
import ee.schimke.okhttp.android.factory.Config
import okhttp3.*
import org.chromium.net.CronetEngine
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

// TODO consider https://github.com/grpc/grpc-java/blob/master/cronet/src/main/java/io/grpc/cronet/CronetChannelBuilder.java
class QuicInterceptor(val quicFilter: (Request) -> Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
            if (installed && quicFilter(chain.request())) {
                executeQuic(chain.call())
            } else {
                chain.proceed(chain.request())
            }

    private fun executeQuic(call: Call): Response {
        val callback = QuicCallback(call)

        val builder = cronetEngine.newUrlRequestBuilder(call.request().url().toString(), callback, executorService).apply {
            this.allowDirectExecutor()
            setHttpMethod(call.request().method())
//            setUploadDataProvider()
        }

        val cReq = builder.build()

        cReq.start()

        try {
            return callback.responseFuture.get()
        } catch (ee: ExecutionException) {
            throw ee.cause as? IOException ?: IOException("failed", ee.cause ?: ee)
        }
    }

    companion object {
        private lateinit var executorService: ExecutorService
        private lateinit var cronetEngine: CronetEngine

        private @Volatile
        var installed: Boolean = false

        fun install(context: Context, executor: ExecutorService, config: Config, listener: (Task<Void>) -> Unit = {}) {
            CronetProviderInstaller.installProvider(context).addOnCompleteListener {
                Log.i("QuicInterceptor", "installed Quic")

                executorService = Executors.newSingleThreadScheduledExecutor(ThreadFactory {
                    Thread(Runnable {
                        try {
                            it.run()
                        } catch (e: Exception) {
                            Log.e("QuicInterceptor", "failed", e)
                        }
                    }, "cronet-1")
                })

                val myBuilder = CronetEngine.Builder(context)
                // Enable caching of HTTP data and
                // other information like QUIC server information, HTTP/2 protocol and QUIC protocol.
                val cacheDir = context.cacheDir.resolve("cronet-cache")
                cacheDir.mkdir()
                cronetEngine = myBuilder
                        .apply {
                            if (config.useCache) {
                                enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK, config.cacheSize / 2)
                            }
                            config.quicHosts.forEach {
                                addQuicHint(it, 443, 443)
                            }
                        }
                        .enableHttp2(false)
                        .enableQuic(true)
                        .setStoragePath(cacheDir.path)
                        .enableBrotli(true)
                        .build()

                installed = true
            }.addOnCompleteListener(listener)
        }
    }
}