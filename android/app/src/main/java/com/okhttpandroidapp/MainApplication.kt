package com.okhttpandroidapp

import android.app.Application
import android.util.Log
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.modules.network.OkHttpClientProvider
import com.facebook.react.modules.network.ReactCookieJarContainer
import com.facebook.soloader.SoLoader
import com.okhttpandroidapp.android.PhoneStatusLiveData
import com.okhttpandroidapp.factory.*
import com.okhttpandroidapp.networks.ConnectionsLiveData
import com.okhttpandroidapp.networks.NetworksLiveData
import com.okhttpandroidapp.reactnative.NetworksPackage
import com.okhttpandroidapp.networks.RequestsLiveData
import com.okhttpandroidapp.reactnative.AppReactNativeHost
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
                    "*.babylonhealth.com",
                    "*.facebook.com",
                    "*.twitter.com",
                    "httpbin.org",
                    "nghttp2.org"),
            cookieJar = ReactCookieJarContainer(),
            conscrypt = false)

    override fun getReactNativeHost(): ReactNativeHost {
        return mReactNativeHost
    }

    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, /* native exopackage */ false)

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

                Log.i("MainApplication", "setOkHttpClientFactory")
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

        Log.i("NetworkStateModule", "Application onCreate")
    }

    override fun onTerminate() {
        androidNetworkManager?.shutdown()
        super.onTerminate()
    }
}
