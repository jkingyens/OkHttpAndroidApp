package com.okhttpandroidapp

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import com.facebook.react.ReactActivity

class MainActivity : ReactActivity() {
    override fun onStart() {
        super.onStart()

        Log.i("NetworkStateModule", "Activity onStart")

        val mainApplication = this.application as MainApplication
        mainApplication.networksPackage.startListeners(this)
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        Log.i("NetworkStateModule", "Activity onCreate")
    }

    override fun getMainComponentName(): String? {
        return "OkHttpAndroidApp"
    }
}
