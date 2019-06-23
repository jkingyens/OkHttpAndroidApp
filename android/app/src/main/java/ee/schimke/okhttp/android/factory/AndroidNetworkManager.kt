package ee.schimke.okhttp.android.factory

import android.app.Application
import android.content.Context
import android.util.Log
import com.babylon.certificatetransparency.Logger
import com.babylon.certificatetransparency.VerificationResult
import com.babylon.certificatetransparency.certificateTransparencyInterceptor
import ee.schimke.okhttp.android.android.*
import ee.schimke.okhttp.android.model.AvailableNetwork
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
import java.net.InetAddress
import java.net.Proxy
import java.net.Socket
import java.net.URI
import java.util.concurrent.TimeUnit


class AndroidNetworkManager(private val application: Application,
                            private val config: Config) : AppForegroundStatusListener {
    val connectionPool = ConnectionPool(10, 5, TimeUnit.MINUTES)
    val networksLiveData = NetworksLiveData(application)
    val requestsLiveData = RequestsLiveData()
    val phoneStatusLiveData = PhoneStatusLiveData(application)
    val availableNetworksLiveData = AvailableNetworksLiveData(application)
    var connectionLiveData = ConnectionsLiveData(connectionPool)
    var threadCall = ThreadLocal<Call>()

    private lateinit var client: OkHttpClient

    private val uriCallMap = mutableMapOf<String, Call>()
    private val networkConnectionMap = mutableMapOf<String, MutableSet<RealConnection>>()
    private val callNetworkMap = mutableMapOf<Call, String?>()
    private val dispatcher = Dispatcher()
    private val dns: Dns = AndroidDns(this)
    private var cache: Cache? = null

    fun initialise(context: Context) {
        if (config.conscrypt) {
            initConscrypt()
        } else if (config.gms) {
            initGms(application)
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
                    if (result !is VerificationResult.Success) {
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
                .eventListenerFactory { NetworkHookEventListener(this, requestsLiveData) }
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
            val networks = t?.networks.orEmpty()

            val availableNetworkIds = networks.filter { it.connected }.map { it.id }
            val droppedNetworkIds = networkConnectionMap.keys.toList().filterNot { availableNetworkIds.contains(it) }

            droppedNetworkIds.forEach { nid ->
                val connections = networkConnectionMap.remove(nid)

                connections?.forEach {
                    Log.i("AndroidNetworkManager", "soft closing ${it.route().socketAddress().hostString} due to network change")

                    it.noNewExchanges()
                    it.socket().closeQuietly()
                }
            }
        }

        AppForegroundStatus.addListener(this)

        // TODO more fine grained foreground/background power control,
        //  e.g. Apps running with active downloads
//        val pm = getSystemService(application, PowerManager::class.java) as PowerManager
//        val partialLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "okhttp:wakelock")
//        partialLock.acquire(30000)
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
            Log.i("AndroidNetworkManager", "$networkConnectionMap")
            networkConnectionMap.connections().forEach {
                Log.i("AndroidNetworkManager", "soft closing ${it.route().socketAddress().hostString} due to background")
                it.noNewExchanges()
            }

            // TODO private val mainScope = MainScope()?
            GlobalScope.launch(Dispatchers.IO) {
                client.connectionPool().evictAll()
            }
        }
    }

    private fun Map<*, MutableSet<RealConnection>>.connections() = this.values.asSequence().flatMap {
        it.asSequence()
    }

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
        val n = callNetwork()

        Log.i("AndroidNetworkManager", "selectLocalSocketAddress ${n?.type}")

        n?.network?.bindSocket(s)
    }

    fun callStart(call: Call) {
        val url = call.request().url().newBuilder().encodedPath("/").build()
        uriCallMap[url.toString()] = call

        Log.i("AndroidNetworkManager", "callStart $url")

        val activeNetworks = activeNetworks(call)

        if (activeNetworks == null || activeNetworks.isNotEmpty()) {
            callNetworkMap[call] = activeNetworks?.first()?.id
        } else {
            call.cancel()
        }
    }

    fun callEnd(call: Call) {
        callNetworkMap.remove(call)
    }

    fun isOfflineFor(call: Call): Boolean {
        val activeNetworks = activeNetworks(call)
        return activeNetworks != null && activeNetworks.isNotEmpty()
    }

    private fun activeNetworks(call: Call?): List<AvailableNetwork>? {
        val networks = availableNetworksLiveData.value?.networks ?: return null

        val orderAndSelect = config.networkSelector.orderAndSelect(networks, call?.request()?.url())

        Log.i("AndroidNetworkManager", "activeNetworks ${networks.map { it.type }} ${orderAndSelect?.map { it.id }}")

        return orderAndSelect
    }

    private fun callNetwork(): AvailableNetwork? {
        val call: Call? = threadCall.get()

        val networkId = callNetworkMap[call]

        Log.i("AndroidNetworkManager", "callNetwork network ${call?.request()?.url()} $networkId")

        return availableNetworksLiveData.value?.networks?.find { it.id == networkId }
    }

    fun lookupDns(hostname: String): List<InetAddress> {
        val n = callNetwork()

        if (n != null) {
            return n.network.getAllByName(hostname).toList()
        }

        return Dns.SYSTEM.lookup(hostname)
    }

    fun connectionAcquired(call: Call, connection: Connection) {
        val nid = callNetworkMap[call]

        if (nid != null) {
            networkConnectionMap.computeIfAbsent(nid) { mutableSetOf() }
                    .add(connection as RealConnection)
        }
    }

    fun selectProxy(uri: URI?): List<Proxy> {
        val call = uriCallMap.remove(uri.toString())

        Log.i("AndroidNetworkManager", "selectProxy $uri ${if (call != null) "" else "nocall"}")

        if (call != null) {
            threadCall.set(call)

            val networks = activeNetworks(call)

            // select the network for call
            val network = networks?.firstOrNull()

            Log.i("AndroidNetworkManager", "selectProxy network ${network?.network}")

            if (network != null) {
                callNetworkMap[call] = network.id

                // TODO use pac file etc?
                return listOf(network.properties.httpProxy?.toProxy() ?: Proxy.NO_PROXY)
            }
        }

        return listOf(Proxy.NO_PROXY)
    }

    fun networkForCall(call: Call): String? {
        return callNetworkMap[call]
    }

    fun linkCall(call: Call) {
        threadCall.set(call)
    }

    fun unlinkCall() {
        threadCall.remove()
    }
}
