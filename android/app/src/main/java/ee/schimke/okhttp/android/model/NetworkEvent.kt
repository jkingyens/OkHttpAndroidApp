package ee.schimke.okhttp.android.model

import java.util.*

data class NetworkEvent(val networkId: String?, val event: String, val id: String = UUID.randomUUID().toString())