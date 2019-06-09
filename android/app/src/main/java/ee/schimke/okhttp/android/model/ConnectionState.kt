package ee.schimke.okhttp.android.model

import okhttp3.Protocol
import okhttp3.TlsVersion

data class ConnectionState(val id: String, val destHost: String, val destPort: Int,
                           val proxy: String?, val host: String, val localAddress: String,
                           val protocol: Protocol, val noNewStreams: Boolean,
                           val tlsVersion: TlsVersion?, val successCount: Int, val network: String)