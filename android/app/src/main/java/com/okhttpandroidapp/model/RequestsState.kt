package com.okhttpandroidapp.model

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

data class RequestsState(val requests: List<CallState>) {
    fun toMap(): WritableMap {
        val result = Arguments.createMap()

        val requestsArray = Arguments.createArray()

        requests.forEach {
            requestsArray.pushMap(it.toMap())
        }

        result.putArray("requests", requestsArray)

        return result
    }
}