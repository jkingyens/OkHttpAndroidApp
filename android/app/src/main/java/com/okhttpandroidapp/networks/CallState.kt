package com.okhttpandroidapp.networks

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import okhttp3.HttpUrl

data class CallState(val url: HttpUrl, val id: Int, val result: Int? = null, val exception: String? = null) {
    fun toMap(): WritableMap {
        return Arguments.createMap().apply {
            putString("id", "" + id)
            putString("url", url.toString())
            if (result != null) {
                putInt("result", result)
            }
            if (exception != null) {
                putString("exception", exception)
            }
        }
    }
}