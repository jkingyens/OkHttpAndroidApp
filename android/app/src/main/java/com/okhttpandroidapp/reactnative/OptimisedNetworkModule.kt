package com.okhttpandroidapp.reactnative

import com.facebook.react.modules.network.OkHttpClientFactory
import com.facebook.react.modules.network.ReactCookieJarContainer
import ee.schimke.okhttp.android.factory.AndroidNetworkManager
import okhttp3.OkHttpClient

internal class OptimisedNetworkModule(private val androidNetworkManager: AndroidNetworkManager)
    : OkHttpClientFactory {
    override fun createNewNetworkModuleClient(): OkHttpClient {
        return androidNetworkManager
                .createOkHttpClient()
                .newBuilder().cookieJar(ReactCookieJarContainer()).build()
    }
}