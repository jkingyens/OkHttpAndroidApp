package ee.schimke.okhttp.android.quic

import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import com.google.android.gms.tasks.Task
import okhttp3.*
import org.chromium.net.CronetEngine
import java.util.concurrent.ExecutorService

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

        val builder = cronetEngine.newUrlRequestBuilder(call.request().url().toString(), callback, executorService)

        val cReq = builder.build()

        cReq.start()

        return callback.response.get()
    }

    companion object {
        private lateinit var executorService: ExecutorService
        private lateinit var cronetEngine: CronetEngine

        private @Volatile
        var installed: Boolean = false

        fun install(context: Context, executor: ExecutorService, listener: (Task<Void>) -> Unit = {}) {
            CronetProviderInstaller.installProvider(context).addOnCompleteListener {
                Log.i("QuicInterceptor", "installed Quic")

                executorService = executor

                val myBuilder = CronetEngine.Builder(context)
                // Enable caching of HTTP data and
                // other information like QUIC server information, HTTP/2 protocol and QUIC protocol.
                cronetEngine = myBuilder
//                        .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, (100 * 1024).toLong())
//                        .enableHttp2(true)
                        .enableQuic(true)
                        .build()

                installed = true
            }.addOnCompleteListener(listener)
        }
    }
}