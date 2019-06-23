package ee.schimke.okhttp.android.factory

import android.net.ProxyInfo
import java.io.IOException
import java.net.*

class AndroidProxySelector(
        private val androidNetworkManager: AndroidNetworkManager,
        val systemSelector: ProxySelector = getDefault()) : ProxySelector() {

    override fun select(uri: URI?): List<Proxy> {
        return androidNetworkManager.selectProxy(uri)
    }

    override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException?) {
        systemSelector.connectFailed(uri, sa, ioe)
    }
}

fun ProxyInfo.toProxy(): Proxy {
    if (this.port == -1) {
        return Proxy.NO_PROXY
    }

    // TODO find using library? exclusions?

    return Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(this.host, this.port))
}