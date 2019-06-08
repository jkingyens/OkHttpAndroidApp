package com.okhttpandroidapp

import android.app.Application
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.modules.network.OkHttpClientProvider
import com.facebook.react.modules.network.ReactCookieJarContainer
import com.facebook.soloader.SoLoader
import com.okhttpandroidapp.android.PhoneStatusLiveData
import com.okhttpandroidapp.factory.AndroidNetworkManager
import com.okhttpandroidapp.factory.Config
import com.okhttpandroidapp.factory.NetworkHookEventListener
import com.okhttpandroidapp.factory.NetworkSelector
import com.okhttpandroidapp.networks.ConnectionsLiveData
import com.okhttpandroidapp.networks.NetworksLiveData
import com.okhttpandroidapp.networks.RequestsLiveData
import com.okhttpandroidapp.reactnative.AppReactNativeHost
import com.okhttpandroidapp.reactnative.NetworksPackage
import com.okhttpandroidapp.reactnative.OptimisedNetworkModule
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Suppress("unused")
class MainApplication : Application(), ReactApplication {
    private var androidNetworkManager: AndroidNetworkManager? = null

    internal lateinit var networksPackage: NetworksPackage
    internal val mReactNativeHost = AppReactNativeHost(this)

    val config = Config(
            optimised = true,
            useCache = false,
            ctHosts = listOf(
                    "*.facebook.com",
                    "*.twitter.com",
                    "httpbin.org",
                    "nghttp2.org"),
            cookieJar = ReactCookieJarContainer(),
            conscrypt = true)

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
