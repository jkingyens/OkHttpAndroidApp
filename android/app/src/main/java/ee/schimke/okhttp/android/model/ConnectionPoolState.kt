package ee.schimke.okhttp.android.model

data class ConnectionPoolState(val connectionCount: Int, val idleConnectionCount: Int,
                               val connections: List<ConnectionState>)