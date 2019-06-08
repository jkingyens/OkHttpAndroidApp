package com.okhttpandroidapp.networks

import com.okhttpandroidapp.model.CallState
import com.okhttpandroidapp.model.RequestsState
import okhttp3.Call
import okhttp3.Response
import java.io.IOException

class RequestsLog() {
    private val requestMap = mutableMapOf<Int, CallState>()
    private var requestId = 0

//    fun toMap(): WritableMap? {
//        val result = Arguments.createMap()
//
//        val requestsArray = Arguments.createArray()
//
//        synchronized(requestMap) {
//            val values = requestMap.values.sortedBy { it.id }
//            values.forEach {
//                requestsArray.pushMap(it.toMap())
//            }
//        }
//
//        result.putArray("requests", requestsArray)
//
//        return result
//    }

    fun start(call: Call) {
        getCallState(call)
    }

    private fun getCallState(call: Call): CallState = synchronized(requestMap) {
        requestMap.computeIfAbsent(System.identityHashCode(call)) {
            CallState(call.request().url(), ++requestId)
        }
    }

    private fun updateCallState(call: Call, fn: (CallState) -> CallState) {
        synchronized(requestMap) {
            requestMap.computeIfPresent(System.identityHashCode(call)) { _, callState ->
                fn(callState)
            }
        }
    }

    fun end(call: Call) {
    }

    fun callSucceeded(call: Call, response: Response) {
        updateCallState(call) {
            it.copy(result = response.code())
        }
    }

    fun callFailed(call: Call, ioe: IOException) {
        updateCallState(call) {
            it.copy(exception = ioe.toString())
        }
    }

    fun requests(): RequestsState {
        val requests = synchronized(requestMap) {
            requestMap.values.sortedBy { it.id }
        }

        return RequestsState(requests)
    }
}