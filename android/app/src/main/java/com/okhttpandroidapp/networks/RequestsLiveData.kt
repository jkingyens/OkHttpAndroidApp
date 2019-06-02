package com.okhttpandroidapp.networks

import android.arch.lifecycle.LiveData
import okhttp3.Call
import okhttp3.Response
import java.io.IOException

class RequestsLiveData : LiveData<RequestsState>() {
    val log = RequestsLog()

    fun callStart(call: Call) {
        log.start(call)

        postValue(log.requests())
    }

    fun callEnd(call: Call) {
        log.end(call)

        postValue(log.requests())
    }

    fun callSucceeded(call: Call, response: Response) {
        log.callSucceeded(call, response)

        postValue(log.requests())
    }

    fun callFailed(call: Call, ioe: IOException) {
        log.callFailed(call, ioe)

        postValue(log.requests())
    }

    fun allRequests(): RequestsState {
        return log.requests()
    }
}