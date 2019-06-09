package ee.schimke.okhttp.android.factory

import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

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