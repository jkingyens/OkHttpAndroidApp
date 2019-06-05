package com.okhttpandroidapp.factory

import java.io.Closeable
import java.net.Socket

/** Closes this, ignoring any checked exceptions. Does nothing if this is null. */
fun Closeable.closeQuietly() {
    try {
        close()
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (_: Exception) {
    }
}

/** Closes this, ignoring any checked exceptions. Does nothing if this is null. */
fun Socket.closeQuietly() {
    try {
        close()
    } catch (e: AssertionError) {
        throw e
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (_: Exception) {
    }
}