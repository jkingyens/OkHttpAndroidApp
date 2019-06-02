package com.okhttpandroidapp.factory

import android.app.Application
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.facebook.react.modules.network.ReactCookieJarContainer
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.okhttpandroidapp.AppSettings
import com.okhttpandroidapp.networks.RequestsLiveData
import okhttp3.*
import org.conscrypt.Conscrypt
import java.net.Socket
import java.net.SocketAddress
import java.security.Security.insertProviderAt
import java.util.concurrent.TimeUnit

class AndroidNetworkManager(private val application: Application,
                            private val networkSelector: NetworkSelector,
                            private val requestsLiveData: RequestsLiveData) {
    private lateinit var client: OkHttpClient
    internal val connectivityManager: ConnectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    internal val wifiManager =
            application.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

    private val networkSocketMap = mutableMapOf<String, MutableList<Socket>>()
    private val availableNetworks = mutableMapOf<String, AvailableNetwork>()
    val connectionPool = ConnectionPool()
    private val dispatcher = Dispatcher()
    // TODO don't assume react native
    private val cookieJar = ReactCookieJarContainer()
    private val dns: Dns = AndroidDns()
    private lateinit var cache: Cache

    fun initialise(context: Context) {
//        if (!initConscrypt()) {
//            initGms(context)
//        }

        client = OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .dns(dns)
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
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
    }

    fun shutdown() {
        Log.i("AndroidNetworkManager", "shutdown")
        dispatcher.executorService().shutdown()
        connectionPool.evictAll()
        cache.flush()
        cache.close()
    }

    private fun initConscrypt(): Boolean {
        try {
            Class.forName("org.conscrypt.OpenSSLProvider")

            Log.w("AndroidNetworkManager", "Initialising Conscrypt")
            if (Conscrypt.isAvailable()) {
                insertProviderAt(
                        Conscrypt.newProviderBuilder().provideTrustManager(true).build(), 1)
                return true
            }
        } catch (e: Exception) {
            Log.w("AndroidNetworkManager", "Conscrypt not available", e)
        }
        return false
    }

    private fun initGms(context: Context): Boolean {
        try {
            ProviderInstaller.installIfNeeded(context)
            return true
        } catch (e: GooglePlayServicesRepairableException) {
            Log.w("AndroidNetworkManager", "Google Play Services repair", e)

            GoogleApiAvailability.getInstance().showErrorNotification(context, e.connectionStatusCode)
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.w("AndroidNetworkManager", "Google Play Services not available", e)
            // ignore
        }
        return false
    }

    fun createOkHttpClient() = client

    fun selectLocalSocketAddress(): SocketAddress? {
        return null
    }

    fun callStart(call: Call) {
//        Log.i("AndroidNetworkManager", "callStart ${call.request().url()}")
    }

    fun callEnd(call: Call) {
//        Log.i("AndroidNetworkManager", "callEnd ${call.request().url()}")
    }

    fun isOfflineFor(url: HttpUrl): Boolean {
        // TODO use same logic
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return (activeNetwork == null || !activeNetwork.isConnected)
    }

    companion object {
        const val CACHE_SIZE_BYTES = 1024 * 1024 * 64L
    }
}