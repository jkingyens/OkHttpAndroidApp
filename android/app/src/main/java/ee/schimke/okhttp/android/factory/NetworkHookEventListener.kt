package ee.schimke.okhttp.android.factory

import android.util.Log
import ee.schimke.okhttp.android.networks.RequestsLiveData
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

class NetworkHookEventListener(val androidNetworkManager: AndroidNetworkManager?,
                               val requestsLiveData: RequestsLiveData)
    : EventListener() {
    override fun callStart(call: Call) {
        Log.i("AndroidNetworkManager-l", "callStart")

        androidNetworkManager?.callStart(call)
        requestsLiveData.callStart(call)
    }

    override fun callEnd(call: Call) {
        Log.i("AndroidNetworkManager-l", "callEnd")

        androidNetworkManager?.callEnd(call)
        requestsLiveData.callEnd(call)
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        Log.i("AndroidNetworkManager-l", "connectionAcquired " + connection.protocol() + " " + connection.route())

        androidNetworkManager?.connectionAcquired(call, connection)
    }

    override fun dnsStart(call: Call, domainName: String) {
        Log.i("AndroidNetworkManager-l", "dnsStart")

        androidNetworkManager?.linkCall(call)
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        Log.i("AndroidNetworkManager-l", "connectStart")
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        val network = androidNetworkManager?.networkForCall(call)
        requestsLiveData.callSucceeded(call, response, network)
    }

    override fun callFailed(call: Call, ioe: IOException) {
        androidNetworkManager?.unlinkCall(call)
        requestsLiveData.callFailed(call, ioe)
    }
}