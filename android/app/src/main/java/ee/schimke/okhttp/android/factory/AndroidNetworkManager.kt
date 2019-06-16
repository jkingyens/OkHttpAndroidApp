package ee.schimke.okhttp.android.factory

import android.app.Application
import android.content.Context
import android.net.ProxyInfo
import android.util.Log
import com.babylon.certificatetransparency.Logger
import com.babylon.certificatetransparency.VerificationResult
import com.babylon.certificatetransparency.certificateTransparencyInterceptor
import ee.schimke.okhttp.android.android.AppForegroundStatus
import ee.schimke.okhttp.android.android.AppForegroundStatusListener
import ee.schimke.okhttp.android.android.PhoneStatusLiveData
import ee.schimke.okhttp.android.android.initConscrypt
import ee.schimke.okhttp.android.model.NetworkEvent
import ee.schimke.okhttp.android.networks.AvailableNetworksLiveData
import ee.schimke.okhttp.android.networks.ConnectionsLiveData
import ee.schimke.okhttp.android.networks.NetworksLiveData
import ee.schimke.okhttp.android.networks.RequestsLiveData
import ee.schimke.okhttp.android.quic.QuicInterceptor
import ee.schimke.okhttp.android.util.closeQuietly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.internal.connection.RealConnection
import java.io.IOException
import java.net.*
import java.util.concurrent.TimeUnit

class AndroidNetworkManager(private val application: Application,
                            private val config: Config,
                            private val networkSelector: NetworkSelector) : AppForegroundStatusListener {
    val connectionPool = ConnectionPool(10, 5, TimeUnit.MINUTES)
    val networksLiveData = NetworksLiveData(application)
    val requestsLiveData = RequestsLiveData()
    val phoneStatusLiveData = PhoneStatusLiveData(application)
    val availableNetworksLiveData = AvailableNetworksLiveData(application)
    var connectionLiveData = ConnectionsLiveData(connectionPool)

    private lateinit var client: OkHttpClient

    private val networkConnectionMap = mutableMapOf<String, MutableList<RealConnection>>()
    private val callNetworkMap = mutableMapOf<Call, String?>()
    private val dispatcher = Dispatcher()
    private val dns: Dns = AndroidDns(this)
    private var cache: Cache? = null

    fun initialise(context: Context) {
        if (config.conscrypt) {
            initConscrypt()
        }

        if (config.quicHosts.isNotEmpty()) {
            QuicInterceptor.install(context, dispatcher.executorService(), config = config) {
                networksLiveData.show(NetworkEvent(null, "Quic installed"))
            }
        }

        val ctIinterceptor = certificateTransparencyInterceptor {
            config.ctHosts.forEach { +it }

            // since it fails http traffic
            failOnError = false
            logger = object : Logger {
                override fun log(host: String, result: VerificationResult) {
                    if (result is VerificationResult.Success) {
                        Log.i("AndroidNetworkManager", "ct: $host $result")
                    } else {
                        Log.w("AndroidNetworkManager", "ct: $host $result")
                    }
                }
            }
        }

        val baseClient = OkHttpClient.Builder()
                .protocols(listOf(Protocol.QUIC, Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .dns(dns)
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .socketFactory(SmartSocketFactory(this))
                .proxySelector(AndroidProxySelector(this))
                .eventListenerFactory { NetworkHookEventListener(this, it, requestsLiveData) }
                // TODO conditional / foreground / background
                .pingInterval(3, TimeUnit.SECONDS)
                .addInterceptor(QuicInterceptor { config.quicHosts.contains(it.url().host()) })
                .addNetworkInterceptor(ctIinterceptor)
                .apply {
                    if (config.useCache) {
                        addInterceptor(UseCacheOfflineInterceptor(this@AndroidNetworkManager))
                        cache = Cache(context.cacheDir.resolve("http-cache"), config.cacheSize)
                        cache(cache)
                    }
                }
                .build()

        client = if (config.doh) {
            val dns = cloudflare(baseClient)
            baseClient.newBuilder().dns(dns).build()
        } else {
            baseClient
        }

        availableNetworksLiveData.observeForever { t ->
            val availableNetworkIds = t?.networks?.filter { it.connected }?.map { it.id }.orEmpty()

            val droppedNetworkIds = networkConnectionMap.keys.toList().filterNot { availableNetworkIds.contains(it) }

            droppedNetworkIds.forEach { nid ->
                val connections = networkConnectionMap.remove(nid)

                connections?.forEach {
                    it.noNewExchanges()
                    it.socket().closeQuietly()
                }
            }
        }

        AppForegroundStatus.addListener(this)
    }

    private fun cloudflare(baseClient: OkHttpClient): DnsOverHttps =
            DnsOverHttps.Builder().client(baseClient)
                    .url(HttpUrl.get("https://1.1.1.1/dns-query"))
                    .build()

    override fun onMoveToForeground() {
        publishPhoneEvent("Foreground")

        if (config.warmedConnections.isNotEmpty()) {
            startConnections(config.warmedConnections)
        }
    }

    private fun startConnections(warmedConnections: List<String>) {
        GlobalScope.launch(Dispatchers.IO) {
            warmedConnections.forEach {
                val req = Request.Builder().url("https://$it/robots.txt").cacheControl(CacheControl.FORCE_NETWORK).build()
                client.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.close()
                    }
                })
            }
        }
    }

    override fun onMoveToBackground() {
        publishPhoneEvent("Background")

        if (config.closeInBackground) {
            networkConnectionMap.connections().forEach {
                it.noNewExchanges()
            }

            // TODO private val mainScope = MainScope()?
            GlobalScope.launch(Dispatchers.IO) {
                client.connectionPool().evictAll()
            }
        }
    }

    private fun Map<*, List<RealConnection>>.connections() = this.values.asSequence().flatMap { it.asSequence() }

    private fun publishPhoneEvent(msg: String) {
        Log.i("AndroidNetworkManager", msg)
        networksLiveData.show(NetworkEvent(null, msg))
    }

    fun shutdown() {
        dispatcher.executorService().shutdown()
        connectionPool.evictAll()
        cache?.flush()
        cache?.close()
    }

    fun createOkHttpClient() = client

    fun selectLocalSocketAddress(s: Socket) {
        val n = activeNetwork()

        if (n != null) {
            n.network.bindSocket(s)
        }
    }

    fun callStart(call: Call) {
        Log.i("AndroidNetworkManager", "callStart ${call.request().url()}")

        callNetworkMap[call] = activeNetwork()?.id
    }

    fun callEnd(call: Call) {
        Log.i("AndroidNetworkManager", "callEnd ${call.request().url()}")
    }

    fun isOfflineFor(url: HttpUrl): Boolean {
        return activeNetwork() == null
    }

    private fun activeNetwork() = availableNetworksLiveData.value?.networks?.firstOrNull { it.connected }

    fun lookupDns(hostname: String): List<InetAddress> {
        val n = activeNetwork()

        if (n != null) {
            return n.network.getAllByName(hostname).toList()
        }

        return Dns.SYSTEM.lookup(hostname)
    }

    fun connectionAcquired(call: Call, connection: Connection) {
        val nid = callNetworkMap[call]

        if (nid != null) {
            networkConnectionMap.computeIfAbsent(nid) { mutableListOf() }
                    .add(connection as RealConnection)
        }
    }

    fun selectProxy(uri: URI?): List<Proxy> {
        val n = activeNetwork()

        if (n != null) {
            val proxy: ProxyInfo? = n.properties.httpProxy

            if (proxy != null) {
                listOf(proxy.toProxy())
            }
        }

        return listOf(Proxy.NO_PROXY)
    }

    fun networkForCall(call: Call): String? {
        return callNetworkMap[call]
    }
}

private fun ProxyInfo.toProxy(): Proxy {
    // TODO find using library? exclusions?

    return Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(this.host, this.port))
}
