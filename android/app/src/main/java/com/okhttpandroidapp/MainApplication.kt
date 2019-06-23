package com.okhttpandroidapp

import android.app.Application
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.modules.network.OkHttpClientProvider
import com.facebook.react.modules.network.ReactCookieJarContainer
import com.facebook.soloader.SoLoader
import com.okhttpandroidapp.reactnative.AppReactNativeHost
import com.okhttpandroidapp.reactnative.NetworksPackage
import com.okhttpandroidapp.reactnative.OptimisedNetworkModule
import ee.schimke.okhttp.android.android.PhoneStatusLiveData
import ee.schimke.okhttp.android.factory.*
import ee.schimke.okhttp.android.model.AvailableNetwork
import ee.schimke.okhttp.android.model.NetworkType
import ee.schimke.okhttp.android.networks.ConnectionsLiveData
import ee.schimke.okhttp.android.networks.NetworksLiveData
import ee.schimke.okhttp.android.networks.RequestsLiveData
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainApplication : Application(), ReactApplication {
    private var androidNetworkManager: AndroidNetworkManager? = null

    internal lateinit var networksPackage: NetworksPackage
    internal val mReactNativeHost = AppReactNativeHost(this)

    val optimised = true

    val CellFirst = object : NetworkSelector {
        override fun orderAndSelect(networks: List<AvailableNetwork>, url: HttpUrl?): List<AvailableNetwork>? {
            val wifi = url?.host()?.contains("facebook") ?: false

            val desired = networks.filter { (it.type == NetworkType.Wifi) == wifi }

            return if (desired.isNotEmpty()) {
                desired
            } else {
                networks
            }
        }
    }

    val config = Config(
            conscrypt = false,
            gms = false,
            useCache = false,
            ctHosts = listOf(
                    "*.facebook.com",
                    "*.twitter.com",
                    "*.google.com",
                    "httpbin.org",
                    "nghttp2.org"),
//            quicHosts = listOf("google.com", "www.google.com"),
            warmedConnections = listOf("facebook.com", "twitter.com"),
//            warmedConnections = listOf("facebook.com", "twitter.com", "api.twitter.com", "graph.facebook.com", "httpbin.org", "nghttp2.org"),
            doh = false,
            networkSelector = CellFirst)

    override fun getReactNativeHost(): ReactNativeHost {
        return mReactNativeHost
    }

    override fun onCreate() {
        initializeNetworks()

        super.onCreate()
        SoLoader.init(this, /* native exopackage */ false)
    }

    private fun initializeNetworks() {
        if (optimised) {
            androidNetworkManager = AndroidNetworkManager(
                    this,
                    config
            ).apply {
                initialise(applicationContext)

                networksPackage = NetworksPackage(
                        networksLiveData,
                        connectionLiveData,
                        requestsLiveData,
                        phoneStatusLiveData)

                OkHttpClientProvider.setOkHttpClientFactory(OptimisedNetworkModule(this))
            }
        } else {
            val connectionPool = ConnectionPool()
            val connectionLiveData = ConnectionsLiveData(connectionPool)

            val requestsLiveData = RequestsLiveData()

            networksPackage = NetworksPackage(
                    NetworksLiveData(this),
                    connectionLiveData,
                    requestsLiveData,
                    PhoneStatusLiveData(this))

            OkHttpClientProvider.setOkHttpClientFactory {
                OkHttpClient.Builder()
                        .connectionPool(connectionPool)
                        .connectTimeout(0, TimeUnit.MILLISECONDS)
                        .readTimeout(0, TimeUnit.MILLISECONDS)
                        .writeTimeout(0, TimeUnit.MILLISECONDS)
                        .cookieJar(ReactCookieJarContainer())
                        .eventListenerFactory { NetworkHookEventListener(null, requestsLiveData) }
                        .build()
            }
        }
    }

    override fun onTerminate() {
        androidNetworkManager?.shutdown()
        super.onTerminate()
    }
}
