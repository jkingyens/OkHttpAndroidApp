package ee.schimke.okhttp.android.networks

import android.arch.lifecycle.LiveData
import ee.schimke.okhttp.android.model.RequestsState
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

    fun callSucceeded(call: Call, response: Response, network: String?) {
        log.callSucceeded(call, response, network = network)

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