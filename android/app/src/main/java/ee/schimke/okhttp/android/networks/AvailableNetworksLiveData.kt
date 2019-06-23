package ee.schimke.okhttp.android.networks

import android.app.Application
import android.arch.lifecycle.LiveData
import android.content.Context
import android.net.*
import android.net.wifi.WifiManager
import android.util.Log
import ee.schimke.okhttp.android.model.AvailableNetwork
import ee.schimke.okhttp.android.model.AvailableNetworks
import ee.schimke.okhttp.android.model.NetworkStatus
import ee.schimke.okhttp.android.model.NetworkType

class AvailableNetworksLiveData(val application: Application) : LiveData<AvailableNetworks>() {
    internal val connectivityManager: ConnectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    internal val wifiManager =
            application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val availableNetworks = mutableMapOf<String, AvailableNetwork>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val availableNetwork = describeNetwork(network)

            synchronized(availableNetworks) {
                availableNetworks[network.toString()] = availableNetwork
            }

            Log.i("AndroidNetworkManager-anld", "onAvailable $network ${availableNetwork.type}")

            postUpdate()
        }

        override fun onUnavailable(): Unit = synchronized(availableNetworks) {
            availableNetworks.clear()
            postUpdate()
        }

        override fun onLost(network: Network): Unit = synchronized(availableNetworks) {
            availableNetworks.remove(network.toString())
            postUpdate()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
        }

        override fun onLosing(network: Network, maxMsToLive: Int): Unit = synchronized(availableNetworks) {
            availableNetworks.computeIfPresent(network.toString()) { _, network ->
                network.copy(status = NetworkStatus.Losing)
            }
            postUpdate()
        }
    }

    private fun describeNetwork(network: Network): AvailableNetwork {
        val info = connectivityManager.getNetworkInfo(network)
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val properties = connectivityManager.getLinkProperties(network)

        val type = NetworkType.toType(capabilities)

        return AvailableNetwork(network.toString(), network, info.isConnected,
                type, NetworkStatus.Available, properties)
    }

    private fun postUpdate() {
        val n = AvailableNetworks(availableNetworks.values.toList())
        postValue(n)
    }

    override fun onActive() {
        val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onInactive() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}