package ee.schimke.okhttp.android.quic

import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import com.google.android.gms.tasks.Task
import okhttp3.*
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService

// TODO consider https://github.com/grpc/grpc-java/blob/master/cronet/src/main/java/io/grpc/cronet/CronetChannelBuilder.java
class QuicInterceptor(val quicFilter: (Request) -> Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (installed && quicFilter(chain.request())) {
            return executeQuic(chain.call(), chain.request())
        } else {
            return chain.proceed(chain.request())
        }
    }

    private fun executeQuic(call: Call, request: Request): Response {
        val callback: UrlRequest.Callback = object : UrlRequest.Callback() {
            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                Log.i("QuicInterceptor", "onResponseStarted " + info.allHeaders)

                request.read(ByteBuffer.allocateDirect(1024 * 1024))
            }

            override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                Log.i("QuicInterceptor", "onReadCompleted")

                request.read(byteBuffer)

//                byteBuffer.flip()

//                byteBuffer.clear()
            }

            override fun onFailed(request: UrlRequest, info: UrlResponseInfo, error: CronetException) {
                Log.i("QuicInterceptor", "onFailed $error")
            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                Log.i("QuicInterceptor", "onSucceeded " + info.httpStatusCode)
            }

            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                Log.i("QuicInterceptor", "onRedirectReceived")

                request.followRedirect();
            }

            override fun onCanceled(request: UrlRequest, info: UrlResponseInfo) {
                Log.i("QuicInterceptor", "onCanceled")
            }
        }

        val builder = cronetEngine.newUrlRequestBuilder(request.url().toString(), callback, executorService)

        val cReq = builder.build()

        cReq.start()

        return Response.Builder()
                .body(ResponseBody.create(MediaType.get("text/plain"), "Hello"))
                .protocol(Protocol.QUIC)
                .request(request)
                .code(200)
                .message("OK")
                .build()
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