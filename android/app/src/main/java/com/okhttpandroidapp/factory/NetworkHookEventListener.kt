package com.okhttpandroidapp.factory

import com.okhttpandroidapp.networks.RequestsLiveData
import okhttp3.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

class NetworkHookEventListener(val androidNetworkManager: AndroidNetworkManager?, val call: Call,
                               val requestsLiveData: RequestsLiveData)
    : EventListener() {
    override fun callStart(call: Call) {
        androidNetworkManager?.callStart(call)
        requestsLiveData.callStart(call)
    }

    override fun callEnd(call: Call) {
        androidNetworkManager?.callEnd(call)
        requestsLiveData.callEnd(call)
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        androidNetworkManager?.connectionAcquired(call, connection)
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        requestsLiveData.callSucceeded(call, response)
    }

    override fun callFailed(call: Call, ioe: IOException) {
        requestsLiveData.callFailed(call, ioe)
    }
}