package com.okhttpandroidapp.factory

import android.net.NetworkCapabilities

enum class NetworkType {
    CellLegacy, Cell3G, CellModern, Wifi, Other;

    companion object {
        fun toType(capabilities: NetworkCapabilities): NetworkType {
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Wifi
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Cell3G
                else -> Other
            }
        }
    }
}