package com.okhttpandroidapp

import android.app.Application
import android.util.Log
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.modules.network.OkHttpClientProvider
import com.facebook.react.modules.network.ReactCookieJarContainer
import com.facebook.react.shell.MainReactPackage
import com.facebook.soloader.SoLoader
import com.okhttpandroidapp.factory.AndroidNetworkManager
import com.okhttpandroidapp.factory.NetworkSelector
import com.okhttpandroidapp.networks.ConnectionsLiveData
import com.okhttpandroidapp.networks.NetworksLiveData
import com.okhttpandroidapp.networks.NetworksPackage
import com.okhttpandroidapp.networks.RequestsLiveData
import com.okhttpandroidapp.reactnative.OptimisedNetworkModule
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("unused")
class MainApplication : Application(), ReactApplication {
    private lateinit var requestsLiveData: RequestsLiveData
    private var androidNetworkManager: AndroidNetworkManager? = null
    internal lateinit var connectionLiveData: ConnectionsLiveData
    internal lateinit var networksPackage: NetworksPackage
    internal lateinit var customNetworkModule: OptimisedNetworkModule
    internal lateinit var networksLiveData: NetworksLiveData

    private val mReactNativeHost = object : ReactNativeHost(this) {
        override fun getUseDeveloperSupport(): Boolean {
            return BuildConfig.DEBUG
        }

        override fun getPackages(): List<ReactPackage> {
            return Arrays.asList(
                    MainReactPackage(),
                    networksPackage
            )
        }

        override fun getJSMainModuleName(): String {
            return "index"
        }
    }

    override fun getReactNativeHost(): ReactNativeHost {
        return mReactNativeHost
    }

    override fun onCreate() {
        networksLiveData = NetworksLiveData(this)
        requestsLiveData = RequestsLiveData()

        if (AppSettings.Optimised) {
            androidNetworkManager = AndroidNetworkManager(
                    this,
                    NetworkSelector.WifiFirst,
                    requestsLiveData
            )
            androidNetworkManager!!.initialise(this.applicationContext)
            connectionLiveData = ConnectionsLiveData(androidNetworkManager!!.connectionPool)

            networksPackage = NetworksPackage(networksLiveData, connectionLiveData, requestsLiveData)

            Log.i("MainApplication", "setOkHttpClientFactory")
            OkHttpClientProvider.setOkHttpClientFactory(OptimisedNetworkModule(androidNetworkManager!!))
        } else {
            TODO()

            val connectionPool = ConnectionPool()
            connectionLiveData = ConnectionsLiveData(connectionPool)
            OkHttpClientProvider.setOkHttpClientFactory {
                Log.i("MainApplication", "createNewNetworkModuleClient")
                OkHttpClient.Builder()
                        .connectionPool(connectionPool)
                        .connectTimeout(0, TimeUnit.MILLISECONDS)
                        .readTimeout(0, TimeUnit.MILLISECONDS)
                        .writeTimeout(0, TimeUnit.MILLISECONDS)
                        .cookieJar(ReactCookieJarContainer())
                        .build()
            }
        }

        super.onCreate()
        SoLoader.init(this, /* native exopackage */ false)
    }

    override fun onTerminate() {
        androidNetworkManager?.shutdown()
        super.onTerminate()
    }
}
