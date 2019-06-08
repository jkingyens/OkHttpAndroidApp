package com.okhttpandroidapp.factory

import android.app.Application
import android.content.Context
import android.net.ProxyInfo
import android.util.Log
import com.babylon.certificatetransparency.Logger
import com.babylon.certificatetransparency.VerificationResult
import com.babylon.certificatetransparency.certificateTransparencyInterceptor
import com.okhttpandroidapp.android.PhoneStatusLiveData
import com.okhttpandroidapp.android.initConscrypt
import com.okhttpandroidapp.networks.ConnectionsLiveData
import com.okhttpandroidapp.networks.NetworksLiveData
import com.okhttpandroidapp.networks.RequestsLiveData
import com.okhttpandroidapp.util.closeQuietly
import okhttp3.*
import okhttp3.internal.connection.RealConnection
import java.net.*
import java.util.concurrent.TimeUnit

class AndroidNetworkManager(private val application: Application,
                            private val config: Config,
                            private val networkSelector: NetworkSelector) {
    val connectionPool = ConnectionPool()
    val networksLiveData = NetworksLiveData(application)
    val requestsLiveData = RequestsLiveData()
    val phoneStatusLiveData = PhoneStatusLiveData(application)
    val availableNetworksLiveData = AvailableNetworksLiveData(application)
    var connectionLiveData = ConnectionsLiveData(connectionPool)

    private lateinit var client: OkHttpClient

    private val networkSocketMap = mutableMapOf<String, MutableList<Socket>>()
    private val networkConnectionMap = mutableMapOf<String, MutableList<RealConnection>>()
    private val dispatcher = Dispatcher()
    private val dns: Dns = AndroidDns(this)
    private var cache: Cache? = null

    fun initialise(context: Context) {
        if (config.conscrypt) {
            initConscrypt()
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

        client = OkHttpClient.Builder()
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
                .addNetworkInterceptor(ctIinterceptor)
                .apply {
                    if (config.cookieJar != null) {
                        cookieJar(config.cookieJar)
                    }
                }
                .apply {
                    if (config.useCache) {
                        addInterceptor(UseCacheOfflineInterceptor(this@AndroidNetworkManager))
                        cache = Cache(context.cacheDir.resolve("http-cache"), config.cacheSize)
                        cache(cache)
                    }
                }
                .build()

        availableNetworksLiveData.observeForever { t ->
            val availableNetworkIds = t?.networks?.filter { it.connected }?.map { it.id }.orEmpty()

            val droppedNetworkIds = networkConnectionMap.keys.toList().filterNot { availableNetworkIds.contains(it) }

            droppedNetworkIds.forEach { nid ->
                val connections = networkConnectionMap.remove(nid)

                connections?.forEach {
                    it.noNewStreams = true
                }

                val sockets = networkSocketMap.remove(nid)

                sockets?.forEach {
                    it.closeQuietly()
                }
            }
        }
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

        Log.i("AndroidNetworkManager", "selectLocalSocketAddress $n")

        if (n != null) {
            n.network.bindSocket(s)
            networkSocketMap.computeIfAbsent(n.id) { mutableListOf() }.add(s)
        }
    }

    fun callStart(call: Call) {
        Log.i("AndroidNetworkManager", "callStart ${call.request().url()}")
    }

    fun callEnd(call: Call) {
        Log.i("AndroidNetworkManager", "callEnd ${call.request().url()}")
    }

    fun isOfflineFor(url: HttpUrl): Boolean {
        val activeNetwork = activeNetwork()

        return activeNetwork == null
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
        val n = activeNetwork()

        if (n != null) {
            networkConnectionMap.computeIfAbsent(n.id) { mutableListOf() }.add(connection as RealConnection)
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
        // TODO confirm logic
        val n = activeNetwork()
        return n?.id
    }
}

private fun ProxyInfo.toProxy(): Proxy {
    // TODO find using library? exclusions?

    return Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(this.host, this.port))
}
