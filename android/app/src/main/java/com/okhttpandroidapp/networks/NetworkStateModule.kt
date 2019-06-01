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
                         val connectionsLiveData: ConnectionsLiveData) : ReactContextBaseJavaModule(reactContext) {
    @ReactMethod
    fun getNetworks(promise: Promise) {
        promise.resolve(networksLiveData.networksState().toMap())
    }

    fun startListeners(lifecycleOwner: LifecycleOwner) {
        val observer = Observer<NetworksState> {
            if (it != null) {
                publishEvent(reactApplicationContext, it)
            }
        }
        networksLiveData.observe(lifecycleOwner, observer)

        val observer2 = Observer<ConnectionPoolState> {
            if (it != null) {
                publishEvent(reactApplicationContext, it)
            }
        }
        connectionsLiveData.observe(lifecycleOwner, observer2)
    }

    @ReactMethod
    fun getConnections(promise: Promise) {
        promise.resolve(connectionsLiveData.readState().toMap())
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: NetworksState) {
//        Log.w("NetworkStateModule", "${state.networks.map { it.name }} ${state.events.lastOrNull()?.event}")
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("networkStateChanged", state.toMap())
    }

    private fun publishEvent(reactContext: ReactApplicationContext, state: ConnectionPoolState) {
//        Log.w("NetworkStateModule", "$state")
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("connectionPoolStateChanged", state.toMap())
    }

    override fun getName(): String {
        return "NetworkState"
    }
}