package ee.schimke.okhttp.android.model

import okhttp3.HttpUrl
import okhttp3.Protocol

data class CallState(val url: HttpUrl,
                     val id: Int,
                     val network: String? = null,
                     val cached: Boolean? = null,
                     val result: Int? = null,
                     val exception: String? = null,
                     val protocol: Protocol? = null)