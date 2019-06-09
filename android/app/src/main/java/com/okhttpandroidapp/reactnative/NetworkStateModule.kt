package com.okhttpandroidapp.reactnative

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import ee.schimke.okhttp.android.android.PhoneStatusLiveData
import ee.schimke.okhttp.android.model.ConnectionPoolState
import ee.schimke.okhttp.android.model.NetworksState
import ee.schimke.okhttp.android.model.PhoneStatus
import ee.schimke.okhttp.android.model.RequestsState
import ee.schimke.okhttp.android.networks.ConnectionsLiveData
import ee.schimke.okhttp.android.networks.NetworksLiveData
import ee.schimke.okhttp.android.networks.RequestsLiveData

class NetworkStateModule(reactContext: ReactApplicationContext,
                         val connectionsLiveData: ConnectionsLiveData,
                         val networksLiveData: NetworksLiveData,
                         val phoneStatusLiveData: PhoneStatusLiveData,
                         val requestsLiveData: RequestsLiveData)
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
        promise.resolve(phoneStatusLiveData.getPhoneStatus().toMap())
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
        emit(reactContext, "phoneStatusChanged", state.toMap())
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