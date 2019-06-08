package com.okhttpandroidapp.factory

import com.okhttpandroidapp.model.AvailableNetwork
import com.okhttpandroidapp.model.NetworkType
import okhttp3.HttpUrl

interface NetworkSelector {
    fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl): List<AvailableNetwork>

    companion object {
        val Default = object : NetworkSelector {
            override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl): List<AvailableNetwork> {
                return networks
            }
        }

        val WifiFirst = object : NetworkSelector {
            override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl): List<AvailableNetwork> {
                val wifi = networks.filter { it.connected && it.type == NetworkType.Wifi }

                return if (wifi.isNotEmpty()) {
                    wifi
                } else {
                    networks
                }
            }
        }
    }
}