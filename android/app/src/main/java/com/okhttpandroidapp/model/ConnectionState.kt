package com.okhttpandroidapp.model

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import okhttp3.Protocol
import okhttp3.TlsVersion

data class ConnectionState(val id: String, val destHost: String, val destPort: Int,
                           val proxy: String?, val host: String, val localAddress: String,
                           val protocol: Protocol, val noNewStreams: Boolean,
                           val tlsVersion: TlsVersion?, val successCount: Int, val network: String) {
    fun toMap(): WritableMap {
        return Arguments.createMap().apply {
            putString("id", id)
            putString("host", host)
            putString("destHost", destHost)
            putInt("destPort", destPort)
            if (proxy != null) {
                putString("proxy", proxy)
            }
            putString("localAddress", localAddress)
            putString("protocol", protocol.toString())
            putBoolean("noNewStreams", noNewStreams)
            if (tlsVersion != null) {
                putString("tlsVersion", tlsVersion.javaName())
            }
            putInt("successCount", successCount)
            putString("network", network)
        }
    }
}
