package com.okhttpandroidapp

import android.os.Handler
import android.util.Log
import com.facebook.react.ReactActivity

class MainActivity : ReactActivity() {

    override fun onStart() {
        super.onStart()

        Handler().postDelayed({
            Log.w("MainActivity", "start network listener")
            val mainApplication = this.application as MainApplication
            try {
                mainApplication.networksPackage.stateModule.startListeners(this)
            } catch (e: UninitializedPropertyAccessException) {
                Log.e("MainActivity", "failed", e)
            }
        }, 2000)
    }

    override fun getMainComponentName(): String? {
        return "OkHttpAndroidApp"
    }
}
