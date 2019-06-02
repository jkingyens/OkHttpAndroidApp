package com.okhttpandroidapp.networks

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

class NetworkStateModule(reactContext: ReactApplicationContext,
                         val networksLiveData: NetworksLiveData,
                         val connectionsLiveData: ConnectionsLiveData,
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
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: NetworksState) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("networkStateChanged", state.toMap())
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: ConnectionPoolState) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("connectionPoolStateChanged", state.toMap())
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: RequestsState) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("requestsChanged", state.toMap())
    }

    override fun getName(): String {
        return "NetworkState"
    }
}