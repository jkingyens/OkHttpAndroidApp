package com.okhttpandroidapp.networks

import android.Manifest
import android.arch.lifecycle.LiveData
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.annotation.RequiresPermission
import okhttp3.ConnectionPool
import okhttp3.Route
import okhttp3.internal.connection.RealConnection
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

        return connections.map { toConnectionState(it) }
    }

    private fun toConnectionState(it: RealConnection): ConnectionState {
        val r = it.route()
        val s = it.socket()
        return ConnectionState(id(s), remoteIp(r), r.socketAddress().port,
                r.proxy().toString(), r.address().url().host(), s.localAddress.hostAddress,
                it.protocol(), it.noNewStreams, it.handshake()?.tlsVersion())
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
        activeTimer = timer(period = 1000) {
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