package com.okhttpandroidapp.model

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

data class PhoneStatus(val powerSave: Boolean, val airplane: Boolean) {
    fun toMap(): WritableMap {
        val result = Arguments.createMap()

        result.putString("airplane", if (airplane) "Airplane" else "")
        result.putString("powerSave", if (powerSave) "Power Save" else "")

        return result
    }
}