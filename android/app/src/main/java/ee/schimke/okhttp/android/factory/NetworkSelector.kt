package ee.schimke.okhttp.android.factory

import ee.schimke.okhttp.android.model.AvailableNetwork
import ee.schimke.okhttp.android.model.NetworkType
import okhttp3.HttpUrl

val Default = object : NetworkSelector {
    override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl?): List<AvailableNetwork>? {
        return null
    }
}

val WifiFirst = object : NetworkSelector {
    override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl?): List<AvailableNetwork>? {
        val wifi = networks.filter { it.connected && it.type == NetworkType.Wifi }

        return if (wifi.isNotEmpty()) {
            wifi
        } else {
            networks
        }
    }
}

val CellFirst = object : NetworkSelector {
    override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl?): List<AvailableNetwork>? {
        val cell = networks.filter { it.connected && it.type != NetworkType.Wifi }

        return if (cell.isNotEmpty()) {
            cell
        } else {
            networks
        }
    }
}

val CellOnly = object : NetworkSelector {
    override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl?): List<AvailableNetwork>? {
        return networks.filter { it.type != NetworkType.Wifi }
    }
}

val Offline = object : NetworkSelector {
    override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl?): List<AvailableNetwork>? {
        return listOf()
    }
}

interface NetworkSelector {
    fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl? = null): List<AvailableNetwork>?
}