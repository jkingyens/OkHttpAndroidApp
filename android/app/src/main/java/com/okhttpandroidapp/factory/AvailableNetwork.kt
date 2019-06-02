package com.okhttpandroidapp.factory

import android.net.Network

data class AvailableNetwork(val network: Network, val connected: Boolean, val type: NetworkType)