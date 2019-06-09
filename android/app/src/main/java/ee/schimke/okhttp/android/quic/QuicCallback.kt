package ee.schimke.okhttp.android.quic

import android.util.Log
import ee.schimke.okhttp.android.factory.eventListener
import okhttp3.*
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class QuicCallback(val call: Call) : UrlRequest.Callback() {
    val response = CompletableFuture<Response>()

    override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
        Log.i("QuicInterceptor", "onResponseStarted")

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
        response.completeExceptionally(error)
    }

    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        Log.i("QuicInterceptor", "onSucceeded " + info.httpStatusCode)

        val newResponse = Response.Builder()
                .body(ResponseBody.create(MediaType.get("text/plain"), "Hello"))
                .protocol(Protocol.QUIC)
                .request(call.request())
                .code(info.httpStatusCode)
                .message(info.httpStatusText)
                .build()

        response.complete(newResponse)
        call.eventListener().responseHeadersEnd(call, newResponse)
    }

    override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
        Log.i("QuicInterceptor", "onRedirectReceived")
        request.followRedirect()
    }

    override fun onCanceled(request: UrlRequest, info: UrlResponseInfo) {
        Log.i("QuicInterceptor", "onCanceled")
    }
}