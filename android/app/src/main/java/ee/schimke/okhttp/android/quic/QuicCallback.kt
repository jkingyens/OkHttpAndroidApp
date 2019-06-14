package ee.schimke.okhttp.android.quic

import android.util.Log
import ee.schimke.okhttp.android.factory.eventListener
import okhttp3.*
import okio.Buffer
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class QuicCallback(val call: Call) : UrlRequest.Callback() {
    private lateinit var response: Response
    val responseFuture = CompletableFuture<Response>()
    val sentRequestMillis = System.currentTimeMillis()
    val buffer = Buffer()
    // TODO free data
    val byteBuffer = ByteBuffer.allocateDirect(64 * 1024)

    override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
        Log.i("QuicInterceptor", "onResponseStarted ${info.negotiatedProtocol}")

        val protocol = if (info.negotiatedProtocol.contains("quic", ignoreCase = true)) Protocol.QUIC else Protocol.HTTP_1_1

        val headers = Headers.Builder().apply {
            info.allHeadersAsList.forEach { (k, v) ->
                add(k, v)
            }
        }.build()

        Log.i("QuicInterceptor", "headers ${headers}")

        // TODO read from setup
        val handshake = Handshake.get(TlsVersion.TLS_1_3, CipherSuite.TLS_AES_128_CCM_SHA256, listOf(), listOf())

        response = Response.Builder()
                .handshake(handshake)
                .sentRequestAtMillis(sentRequestMillis)
                .receivedResponseAtMillis(System.currentTimeMillis())
                .protocol(protocol)
                .request(call.request())
                .code(info.httpStatusCode)
                .message(info.httpStatusText)
                .headers(headers)
                .build()

        call.eventListener.responseHeadersEnd(call, response)

        // TODO check info.httpStatusCode and response bytes etc
        request.read(byteBuffer)
    }

    override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
        Log.i("QuicInterceptor", "onReadCompleted")

        byteBuffer.flip()

        buffer.write(byteBuffer)

        byteBuffer.clear()
        request.read(byteBuffer)
    }

    override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException?) {
        Log.i("QuicInterceptor", "onFailed $error")
        responseFuture.completeExceptionally(error)
    }

    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        Log.i("QuicInterceptor", "onSucceeded " + info.httpStatusCode)

        val contentType = response.header("content-type").let {
            MediaType.parse(it)
        }
        val bytes = buffer.readByteString()
        Log.i("QuicInterceptor", "content-type: $contentType ${bytes.size()}")
//        Log.i("QuicInterceptor", "body: " + bytes.string(StandardCharsets.UTF_8))
        response = response.newBuilder().body(ResponseBody.create(contentType, bytes)).build()

        responseFuture.complete(response)
    }

    override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
        Log.i("QuicInterceptor", "onRedirectReceived")

        // TODO match OkHttp config
        request.followRedirect()
    }

    override fun onCanceled(request: UrlRequest, info: UrlResponseInfo) {
        Log.i("QuicInterceptor", "onCanceled")
    }
}