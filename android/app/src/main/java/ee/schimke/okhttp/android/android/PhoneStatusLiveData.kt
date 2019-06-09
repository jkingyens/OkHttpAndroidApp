package ee.schimke.okhttp.android.android

import android.app.Application
import android.arch.lifecycle.LiveData
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import ee.schimke.okhttp.android.model.PhoneStatus
import java.util.*
import kotlin.concurrent.timer

class PhoneStatusLiveData(val application: Application) : LiveData<PhoneStatus>() {
    private var lastState: PhoneStatus? = null
    private var activeTimer: Timer? = null

    var powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun getPhoneStatus(): PhoneStatus {
        val airplane = Settings.Global.getInt(application.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        val powerSave = powerManager.isPowerSaveMode

        return PhoneStatus(powerSave, airplane)
    }

    private fun update() {
        val newState = getPhoneStatus()

        if (newState != lastState) {
            postValue(newState)
            lastState = newState
        }
    }

    override fun onActive() {
        activeTimer = timer(period = 500) {
            update()
        }
    }

    override fun onInactive() {
        activeTimer?.cancel()
    }
}