package com.okhttpandroidapp

import com.facebook.react.ReactActivity

class MainActivity : ReactActivity() {
    override fun onStart() {
        super.onStart()

        val mainApplication = this.application as MainApplication
        mainApplication.networksPackage.startListeners(this)
    }

    override fun getMainComponentName(): String? {
        return "OkHttpAndroidApp"
    }
}
