package com.okhttpandroidapp

import android.os.Handler
import android.util.Log
import com.facebook.react.ReactActivity

class MainActivity : ReactActivity() {

    override fun onStart() {
        super.onStart()

        backgroundStartListeners()
    }

    private fun backgroundStartListeners() {
        Handler().postDelayed({
            Log.w("MainActivity", "attempting to start network listeners")
            val mainApplication = this.application as MainApplication

            if (mainApplication.networksPackage.isInitialised()) {
                try {
                    mainApplication.networksPackage.stateModule.startListeners(this)
                } catch (e: UninitializedPropertyAccessException) {
                    Log.e("MainActivity", "failed", e)
                }
            } else {
                Log.w("MainActivity", "retry in 0.5 seconds")
                backgroundStartListeners()
            }
        }, 2000) // This is a pure hack
    }

    override fun getMainComponentName(): String? {
        return "OkHttpAndroidApp"
    }
}
