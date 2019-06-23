package ee.schimke.okhttp.android.networks

import android.app.Application
import android.arch.lifecycle.LiveData
import android.content.Context
import android.net.*
import android.support.annotation.RequiresPermission
import ee.schimke.okhttp.android.model.NetworkEvent
import ee.schimke.okhttp.android.model.NetworkState
import ee.schimke.okhttp.android.model.NetworksState
import org.apache.commons.lang3.StringUtils
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

class NetworksLiveData
@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
constructor(application: Application)
    : LiveData<NetworksState>() {
    private val events = mutableListOf<NetworkEvent>()
    private val connectivityManager: ConnectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val lastProperties = mutableMapOf<String, LinkProperties>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            show(NetworkEvent(network.toString(), "available"))
        }

        override fun onUnavailable() {
            show(NetworkEvent(null, "unavailable"))
        }

        override fun onLost(network: Network) {
            show(NetworkEvent(network.toString(), "lost"))
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            val last = synchronized(lastProperties) { lastProperties[network.toString()] }

            if (last != null && last != linkProperties) {
                val diff = StringUtils.difference(last.toString(), linkProperties.toString())

                show(NetworkEvent(network.toString(), "properties changed: $diff"))
            }

            synchronized(lastProperties) {
                lastProperties[network.toString()] = linkProperties
            }
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            show(NetworkEvent(network.toString(), "losing in $maxMsToLive ms"))
        }
    }

    fun show(networkEvent: NetworkEvent) {
        synchronized(events) {
            this.events.add(networkEvent)
        }

        postValue(networksState())
    }

    fun networksState(): NetworksState {
        val activeNetwork = connectivityManager.activeNetwork?.toString()
        val networks = connectivityManager.allNetworks.map { describe(it, activeNetwork == it.toString()) }
        val eventsCopy = synchronized(events) { events.toList() }

        return NetworksState(networks, eventsCopy, activeNetwork)
    }

    @Suppress("DEPRECATION")
    private fun describe(network: Network, active: Boolean): NetworkState {
        val info: NetworkInfo? = connectivityManager.getNetworkInfo(network)
        val capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
        val properties: LinkProperties? = connectivityManager.getLinkProperties(network)

        val localAddress = properties?.linkAddresses?.minBy { it.address is Inet6Address }?.address.toString()

        return NetworkState(network.toString(), properties?.interfaceName
                ?: "unknown", info?.typeName + "/" + info?.subtypeName, info?.isConnected,
                info?.detailedState.toString(), capabilities?.linkDownstreamBandwidthKbps,
                capabilities?.linkUpstreamBandwidthKbps, active,
                localAddress)
    }

    override fun onActive() {
        val request = NetworkRequest.Builder().addCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onInactive() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}