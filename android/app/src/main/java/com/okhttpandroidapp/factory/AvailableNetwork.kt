package com.okhttpandroidapp.factory

import android.net.LinkProperties
import android.net.Network

data class AvailableNetwork(val id: String, val network: Network, val connected: Boolean,
                            val type: NetworkType, val status: NetworkStatus,
                            val properties: LinkProperties)