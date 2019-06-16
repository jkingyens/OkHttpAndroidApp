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
import ee.schimke.okhttp.android.factory.AndroidNetworkManager
import ee.schimke.okhttp.android.factory.Config
import ee.schimke.okhttp.android.factory.NetworkHookEventListener
import ee.schimke.okhttp.android.factory.NetworkSelector
import ee.schimke.okhttp.android.networks.ConnectionsLiveData
import ee.schimke.okhttp.android.networks.NetworksLiveData
import ee.schimke.okhttp.android.networks.RequestsLiveData
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainApplication : Application(), ReactApplication {
    private var androidNetworkManager: AndroidNetworkManager? = null

    internal lateinit var networksPackage: NetworksPackage
    internal val mReactNativeHost = AppReactNativeHost(this)

    val config = Config(
            useCache = false,
            ctHosts = listOf(
                    "*.facebook.com",
                    "*.twitter.com",
                    "*.google.com",
                    "httpbin.org",
                    "nghttp2.org"),
            quicHosts = listOf("google.com", "www.google.com"),
            warmedConnections = listOf("facebook.com", "twitter.com", "api.twitter.com", "graph.facebook.com", "httpbin.org", "nghttp2.org"))

    override fun getReactNativeHost(): ReactNativeHost {
        return mReactNativeHost
    }

    override fun onCreate() {
        initializeNetworks()

        super.onCreate()
        SoLoader.init(this, /* native exopackage */ false)
    }

    private fun initializeNetworks() {
        if (config.optimised) {
            androidNetworkManager = AndroidNetworkManager(
                    this,
                    config,
                    NetworkSelector.WifiFirst
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
                        .eventListenerFactory { NetworkHookEventListener(null, it, requestsLiveData) }
                        .build()
            }
        }
    }

    override fun onTerminate() {
        androidNetworkManager?.shutdown()
        super.onTerminate()
    }
}
