package ee.schimke.okhttp.android.android

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner

object AppForegroundStatus {
    fun addListener(listener: AppForegroundStatusListener) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onMoveToForeground() {
                listener.onMoveToForeground()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onMoveToBackground() {
                listener.onMoveToBackground()
            }
        })
    }
}

interface AppForegroundStatusListener {
    fun onMoveToForeground()

    fun onMoveToBackground()
}