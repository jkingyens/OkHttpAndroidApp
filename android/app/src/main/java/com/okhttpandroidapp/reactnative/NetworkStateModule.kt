package com.okhttpandroidapp.reactnative

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.okhttpandroidapp.android.PhoneStatusLiveData
import com.okhttpandroidapp.model.ConnectionPoolState
import com.okhttpandroidapp.model.NetworksState
import com.okhttpandroidapp.model.PhoneStatus
import com.okhttpandroidapp.model.RequestsState
import com.okhttpandroidapp.networks.ConnectionsLiveData
import com.okhttpandroidapp.networks.NetworksLiveData
import com.okhttpandroidapp.networks.RequestsLiveData

class NetworkStateModule(reactContext: ReactApplicationContext,
                         val networksLiveData: NetworksLiveData,
                         val connectionsLiveData: ConnectionsLiveData,
                         val requestsLiveData: RequestsLiveData,
                         val phoneStatusLiveData: PhoneStatusLiveData)
    : ReactContextBaseJavaModule(reactContext) {

    @ReactMethod
    fun getNetworks(promise: Promise) {
        promise.resolve(networksLiveData.networksState().toMap())
    }

    @ReactMethod
    fun getConnections(promise: Promise) {
        promise.resolve(connectionsLiveData.readState().toMap())
    }

    @ReactMethod
    fun getRequests(promise: Promise) {
        promise.resolve(requestsLiveData.allRequests().toMap())
    }

    @ReactMethod
    fun getPhoneStatus(promise: Promise) {
        promise.resolve(phoneStatusLiveData.getPhoneStatus())
    }

    fun startListeners(lifecycleOwner: LifecycleOwner) {
        networksLiveData.observe(lifecycleOwner, Observer {
            if (it != null) {
                publishEvent(reactApplicationContext, it)
            }
        })

        connectionsLiveData.observe(lifecycleOwner, Observer {
            if (it != null) {
                publishEvent(reactApplicationContext, it)
            }
        })

        requestsLiveData.observe(lifecycleOwner, Observer {
            if (it != null) {
                publishEvent(reactApplicationContext, it)
            }
        })

        phoneStatusLiveData.observe(lifecycleOwner, Observer {
            if (it != null) {
                publishEvent(reactApplicationContext, it)
            }
        })
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: NetworksState) {
        emit(reactContext, "networkStateChanged", state.toMap())
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: ConnectionPoolState) {
        emit(reactContext, "connectionPoolStateChanged", state.toMap())
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: RequestsState) {
        emit(reactContext, "requestsChanged", state.toMap())
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: PhoneStatus) {
        val event = "phoneStatusChanged"
        val data = state.toMap()
        emit(reactContext, event, data)
    }

    private fun emit(reactContext: ReactApplicationContext, event: String, data: WritableMap) {
        // TODO unravel this ordering
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(event, data)
        }
    }

    override fun getName(): String {
        return "NetworkState"
    }
}