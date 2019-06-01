package com.okhttpandroidapp.networks

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import okhttp3.Protocol
import okhttp3.TlsVersion

data class ConnectionState(val id: String, val destHost: String, val destPort: Int,
                           val proxy: String, val host: String, val localAddress: String,
                           val protocol: Protocol, val noNewStreams: Boolean,
                           val tlsVersion: TlsVersion?) {
    fun toMap(): WritableMap {
        return Arguments.createMap().apply {
            putString("id", id)
            putString("destHost", destHost)
            putInt("destPort", destPort)
            putString("proxy", proxy)
            putString("host", host)
            putString("localAddress", localAddress)
            putString("protocol", protocol.toString())
            putBoolean("noNewStreams", noNewStreams)
            putString("tlsVersion", tlsVersion.toString())
        }
    }
}
