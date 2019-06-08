package com.okhttpandroidapp.reactnative

import android.util.Log
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.shell.MainReactPackage
import com.okhttpandroidapp.BuildConfig
import com.okhttpandroidapp.MainApplication

class AppReactNativeHost(val application: MainApplication) : ReactNativeHost(application) {
    override fun getUseDeveloperSupport(): Boolean {
        return BuildConfig.DEBUG
    }

    override fun getPackages(): List<ReactPackage> {
        Log.i("NetworkStateModule", "host getPackages")

        return listOf(
                MainReactPackage(),
                application.networksPackage
        )
    }

    override fun getJSMainModuleName(): String {
        return "index"
    }
}