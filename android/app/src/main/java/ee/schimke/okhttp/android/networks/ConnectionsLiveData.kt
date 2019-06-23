package ee.schimke.okhttp.android.networks

import android.Manifest
import android.arch.lifecycle.LiveData
import android.support.annotation.RequiresPermission
import android.util.Log
import ee.schimke.okhttp.android.factory.listConnections
import ee.schimke.okhttp.android.factory.noNewExchangesF
import ee.schimke.okhttp.android.factory.successCountF
import ee.schimke.okhttp.android.model.ConnectionPoolState
import ee.schimke.okhttp.android.model.ConnectionState
import okhttp3.ConnectionPool
import okhttp3.Route
import okhttp3.internal.connection.RealConnection
import java.net.Proxy
import java.net.Socket
import java.util.*
import kotlin.concurrent.timer

class ConnectionsLiveData
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
constructor(val connectionPool: ConnectionPool)
    : LiveData<ConnectionPoolState>() {

    private lateinit var activeTimer: Timer
    private var lastState: ConnectionPoolState? = null
    private val hashCodeToConnectionMap = mutableMapOf<Int, String>()
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
        val connections = connectionPool.listConnections().map { it.toConnectionState() }

        val knownConnections = connections.map { it.id }.toSet()
        val droppedConnections = hashCodeToConnectionMap.filterNot { (_, connectionId) ->
            knownConnections.contains(connectionId)
        }
        droppedConnections.forEach {(hashCode, _) ->
            hashCodeToConnectionMap.remove(hashCode)
        }

        return ConnectionPoolState(connectionPool.connectionCount(),
                connectionPool.idleConnectionCount(), connections)
    }

    private fun RealConnection.toConnectionState(): ConnectionState {
        val r = route()
        val s = socket()
        val proxy = r.proxy()
        val localIpAddress = s.localAddress.hostAddress
        // TODO populate
        val networkId: String? = null
        return ConnectionState(id(s), remoteIp(r), r.socketAddress().port,
                if (proxy != Proxy.NO_PROXY) proxy.toString() else null,
                r.address().url().host(), localIpAddress,
                protocol(), this.noNewExchangesF, handshake()?.tlsVersion(), this.successCountF,
                networkId
        )
    }

    private fun id(s: Socket): String {
        return hashCodeToConnectionMap.computeIfAbsent(System.identityHashCode(s)) {
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
}