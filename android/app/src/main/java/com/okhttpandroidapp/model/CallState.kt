package com.okhttpandroidapp.model

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import okhttp3.HttpUrl

data class CallState(val url: HttpUrl,
                     val id: Int,
                     val network: String? = null,
                     val cached: Boolean? = null,
                     val result: Int? = null,
                     val exception: String? = null) {
    fun toMap(): WritableMap {
        return Arguments.createMap().apply {
            putString("id", id.toString())
            putString("url", url.toString())
            if (network != null) {
                putString("network", "" + network)
            }
            if (cached != null) {
                putString("source", if (cached) "C" else "N")
            }
            if (result != null) {
                putInt("result", result)
            }
            if (exception != null) {
                putString("exception", exception)
            }
        }
    }
}