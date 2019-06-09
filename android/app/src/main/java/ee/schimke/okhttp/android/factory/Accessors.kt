package ee.schimke.okhttp.android.factory

import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.internal.connection.RealConnection
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


@Suppress("UNCHECKED_CAST")
val connectionsProperty = (ConnectionPool::class.memberProperties
        .find { it.name == "connections" }!!
        as KProperty1<ConnectionPool, Deque<RealConnection>>).apply {
    isAccessible = true
}

fun ConnectionPool.listConnections(): Collection<RealConnection> = connectionsProperty.get(this)

val realCallClass: KClass<Call> = Class.forName("okhttp3.RealCall").kotlin as KClass<Call>

@Suppress("UNCHECKED_CAST")
val listenerProperty = realCallClass
        .memberProperties.find { it.name == "eventListener" }!!.apply {
    isAccessible = true
}

fun Call.eventListener() = listenerProperty.get(this) as okhttp3.EventListener
