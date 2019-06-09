package com.okhttpandroidapp.reactnative

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import ee.schimke.okhttp.android.model.*


fun CallState.toMap(): WritableMap {
    return Arguments.createMap().apply {
        putString("id", id.toString())
        putString("url", url.toString())
        if (network != null) {
            putString("network", "" + network)
        }
        if (cached != null) {
            putString("source", if (cached) "C" else "N")
        }
        if (result != null) {
            putInt("result", result)
        }
        if (exception != null) {
            putString("exception", exception)
        }
    }
}

fun ConnectionPoolState.toMap(): WritableMap {
    val result = Arguments.createMap()

    val connectionsArray = Arguments.createArray()
    connections.forEach {
        connectionsArray.pushMap(it.toMap())
    }

    result.putInt("connectionsCount", connectionCount)
    result.putInt("idleConnectionsCount", idleConnectionCount)
    result.putArray("connections", connectionsArray)

    return result
}

fun ConnectionState.toMap(): WritableMap {
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

fun NetworkEvent.toMap(): WritableMap {
    return Arguments.createMap().apply {
        putString("id", id)
        putString("networkId", networkId)
        putString("event", event)
    }
}

fun NetworksState.toMap(): WritableMap {
    val result = Arguments.createMap()

    val networksArray = Arguments.createArray()
    networks.forEach {
        networksArray.pushMap(it.toMap())
    }

    val eventsArray = Arguments.createArray()
    events.forEach {
        eventsArray.pushMap(it.toMap())
    }

    result.putArray("networks", networksArray)
    result.putArray("events", eventsArray)
    result.putString("activeNetwork", activeNetwork)

    return result
}

fun NetworkState.toMap(): WritableMap {
    return Arguments.createMap().apply {
        putString("networkId", networkId)
        putString("name", name)
        putString("type", type)
        putBoolean("connected", connected ?: false)
        putString("state", state)
        putInt("downstreamKbps", downstreamKbps ?: -1)
        putInt("upstreamKbps", upstreamKbps ?: -1)
        putBoolean("active", active)
        putString("localAddress", localAddress)
    }
}

fun PhoneStatus.toMap(): WritableMap {
    val result = Arguments.createMap()

    result.putString("airplane", if (airplane) "Airplane" else "")
    result.putString("powerSave", if (powerSave) "Power Save" else "")

    return result
}

fun RequestsState.toMap(): WritableMap {
    val result = Arguments.createMap()

    val requestsArray = Arguments.createArray()

    requests.forEach {
        requestsArray.pushMap(it.toMap())
    }

    result.putArray("requests", requestsArray)

    return result
}
