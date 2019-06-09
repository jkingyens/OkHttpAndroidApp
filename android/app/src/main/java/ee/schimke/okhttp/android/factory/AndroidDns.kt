package ee.schimke.okhttp.android.factory

import okhttp3.Dns
import java.net.InetAddress

class AndroidDns(val androidNetworkManager: AndroidNetworkManager): Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return androidNetworkManager.lookupDns(hostname)
    }
}