package com.okhttpandroidapp.factory

import android.app.Application
import android.arch.lifecycle.Observer
import android.content.Context
import android.net.ProxyInfo
import android.util.Log
import com.facebook.react.modules.network.ReactCookieJarContainer
import com.okhttpandroidapp.android.initConscrypt
import com.okhttpandroidapp.networks.RequestsLiveData
import okhttp3.*
import okhttp3.internal.connection.RealConnection
import java.net.*
import java.util.concurrent.TimeUnit

class AndroidNetworkManager(private val application: Application,
                            private val networkSelector: NetworkSelector,
                            private val requestsLiveData: RequestsLiveData,
                            private val availableNetworksLiveData: AvailableNetworksLiveData
) {
    private lateinit var client: OkHttpClient

    private val networkSocketMap = mutableMapOf<String, MutableList<Socket>>()
    private val networkConnectionMap = mutableMapOf<String, MutableList<RealConnection>>()
    val connectionPool = ConnectionPool()
    private val dispatcher = Dispatcher()
    // TODO don't assume react native
    private val cookieJar = ReactCookieJarContainer()
    private val dns: Dns = AndroidDns(this)
    private lateinit var cache: Cache

    fun initialise(context: Context) {
        initConscrypt()

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
                .cookieJar(cookieJar)
//                .apply {
//                    if (AppSettings.Cache) {
//        addInterceptor(UseCacheOfflineInterceptor(this))
//                        cache = Cache(context.cacheDir.resolve("http-cache"), CACHE_SIZE_BYTES)
//                        cache(cache)
//                    }
//                }
                .build()

        availableNetworksLiveData.observeForever { t ->
            val availableNetworkIds = t?.networks?.filter { it.connected }?.map { it.id }.orEmpty()

            val droppedNetworkIds = networkConnectionMap.keys.toList().filterNot { availableNetworkIds.contains(it) }

            Log.i("AndroidNetworkManager", "change networks available ${availableNetworkIds}")
            Log.i("AndroidNetworkManager", "change networks connections ${networkConnectionMap}")

            Log.i("AndroidNetworkManager", "dropping $droppedNetworkIds")

            droppedNetworkIds.forEach {nid ->
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
        cache.flush()
        cache.close()
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

        Log.i("AndroidNetworkManager", "isOfflineFor $url $activeNetwork")

        return activeNetwork == null
    }

    private fun activeNetwork() = availableNetworksLiveData.value?.networks?.firstOrNull { it.connected }

    fun lookupDns(hostname: String): List<InetAddress> {
        val n = activeNetwork()

        Log.i("AndroidNetworkManager", "lookupDns $hostname $n")

        if (n != null) {
            return n.network.getAllByName(hostname).toList()
        }

        return Dns.SYSTEM.lookup(hostname)
    }

    fun connectionAcquired(call: Call, connection: Connection) {
        val n = activeNetwork()

        Log.i("AndroidNetworkManager", "connectionAcquired ${call.request().url()} $n")

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

    companion object {
        const val CACHE_SIZE_BYTES = 1024 * 1024 * 64L
    }
}

private fun ProxyInfo.toProxy(): Proxy {
    // TODO find using library? exclusions?

    return Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(this.host, this.port))
}
