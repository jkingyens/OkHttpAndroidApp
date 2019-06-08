package com.okhttpandroidapp.networks

import android.Manifest
import android.arch.lifecycle.LiveData
import android.support.annotation.RequiresPermission
import com.okhttpandroidapp.model.ConnectionPoolState
import com.okhttpandroidapp.model.ConnectionState
import okhttp3.ConnectionPool
import okhttp3.Route
import okhttp3.internal.connection.RealConnection
import java.net.Proxy
import java.net.Socket
import java.util.*
import kotlin.concurrent.timer
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class ConnectionsLiveData
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
constructor(val connectionPool: ConnectionPool)
    : LiveData<ConnectionPoolState>() {

    private lateinit var activeTimer: Timer
    private var lastState: ConnectionPoolState? = null
    private val connectionMap = mutableMapOf<Int, String>()
    private var connectionId = 0

    init {
        update()
    }

    private fun update() {
        val newState = readState()

        if (newState != lastState) {
            postValue(newState)
            lastState = newState
        }
    }

    fun readState(): ConnectionPoolState {
        val connections = listConnections()
        val newState = ConnectionPoolState(connectionPool.connectionCount(),
                connectionPool.idleConnectionCount(), connections)
        return newState
    }

    private fun listConnections(): List<ConnectionState> {
        val connections: Collection<RealConnection> = connectionsProperty.get(connectionPool)

        return connections.map { it.toConnectionState() }
    }

    private fun RealConnection.toConnectionState(): ConnectionState {
        val r = route()
        val s = socket()
        // TODO fix
        val network = "-1"
        val proxy = r.proxy()
        return ConnectionState(id(s), remoteIp(r), r.socketAddress().port,
                if (proxy != Proxy.NO_PROXY) proxy.toString() else null,
                r.address().url().host(), s.localAddress.hostAddress,
                protocol(), noNewStreams, handshake()?.tlsVersion(), successCount,
                network)
    }

    private fun id(s: Socket): String {
        return connectionMap.computeIfAbsent(System.identityHashCode(s)) {
            "" + ++connectionId
        }
    }

    private fun remoteIp(r: Route): String {
        val socketAddress = r.socketAddress().address
        return socketAddress.hostAddress
    }

    override fun onActive() {
        activeTimer = timer(period = 500) {
            update()
        }
    }

    override fun onInactive() {
        activeTimer.cancel()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val connectionsProperty = (ConnectionPool::class.memberProperties
                .find { it.name == "connections" }!!
                as KProperty1<ConnectionPool, Deque<RealConnection>>).apply {
            isAccessible = true
        }
    }
}