package ee.schimke.okhttp.android.util

import java.io.Closeable
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException

/** Closes this, ignoring any checked exceptions. Does nothing if this is null. */
fun Closeable.closeQuietly() {
    try {
        close()
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (_: Exception) {
    }
}

/** Closes this, ignoring any checked exceptions. Does nothing if this is null. */
fun Socket.closeQuietly() {
    try {
        close()
    } catch (e: AssertionError) {
        throw e
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (_: Exception) {
    }
}


fun getByIp(host: String): InetAddress {
    try {
        return InetAddress.getByName(host)
    } catch (e: UnknownHostException) {
        // unlikely
        throw RuntimeException(e)
    }

}