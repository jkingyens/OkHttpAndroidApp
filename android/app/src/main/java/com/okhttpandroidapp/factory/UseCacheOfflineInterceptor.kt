package com.okhttpandroidapp.factory

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

class UseCacheOfflineInterceptor(private val androidNetworkManager: AndroidNetworkManager): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().run {
            val offline = androidNetworkManager.isOfflineFor(chain.request().url())

            if (offline) {
                newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
            } else {
                this
            }
        }

        return chain.proceed(request)
    }
}