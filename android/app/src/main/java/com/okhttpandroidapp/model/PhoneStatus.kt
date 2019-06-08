package com.okhttpandroidapp.model

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

data class PhoneStatus(val powerSave: Boolean, val airplane: Boolean) {
    fun toMap(): WritableMap {
        val result = Arguments.createMap()

        result.putBoolean("airplane", airplane)
        result.putBoolean("powerSave", powerSave)

        return result
    }
}