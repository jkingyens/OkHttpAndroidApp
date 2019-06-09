package ee.schimke.okhttp.android.model

data class NetworksState(val networks: List<NetworkState>, val events: List<NetworkEvent>,
                         val activeNetwork: String?)