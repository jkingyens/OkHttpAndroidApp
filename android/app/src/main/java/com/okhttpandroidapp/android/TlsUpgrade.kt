package com.okhttpandroidapp.android

import android.content.Context
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import org.conscrypt.Conscrypt
import java.security.Security


fun initConscrypt(): Boolean {
    try {
        Class.forName("org.conscrypt.OpenSSLProvider")

        Log.w("AndroidNetworkManager", "Initialising Conscrypt")
        if (Conscrypt.isAvailable()) {
            Security.insertProviderAt(
                    Conscrypt.newProviderBuilder().provideTrustManager(true).build(), 1)
            return true
        }
    } catch (e: Exception) {
        Log.w("AndroidNetworkManager", "Conscrypt not available", e)
    }
    return false
}

fun initGms(context: Context): Boolean {
    try {
        ProviderInstaller.installIfNeeded(context)
        return true
    } catch (e: GooglePlayServicesRepairableException) {
        Log.w("AndroidNetworkManager", "Google Play Services repair", e)

        GoogleApiAvailability.getInstance().showErrorNotification(context, e.connectionStatusCode)
    } catch (e: GooglePlayServicesNotAvailableException) {
        Log.w("AndroidNetworkManager", "Google Play Services not available", e)
        // ignore
    }
    return false
}