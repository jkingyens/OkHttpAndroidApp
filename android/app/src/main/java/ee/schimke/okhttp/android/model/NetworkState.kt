package ee.schimke.okhttp.android.model

data class NetworkState(val networkId: String?, val name: String, val type: String?,
                        val connected: Boolean?, val state: String?, val downstreamKbps: Int?,
                        val upstreamKbps: Int?, val active: Boolean, val localAddress: String?)
