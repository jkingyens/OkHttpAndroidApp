package ee.schimke.okhttp.android.factory

import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.EventListener
import okhttp3.internal.connection.RealConnection
import okhttp3.internal.connection.RealConnectionPool
import okhttp3.internal.connection.Transmitter
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

// ConnectionPool

@Suppress("UNCHECKED_CAST")
val delegateProperty = (ConnectionPool::class.memberProperties
        .find { it.name == "delegate" }!!
        as KProperty1<ConnectionPool, RealConnectionPool>).apply {
    isAccessible = true
}

@Suppress("UNCHECKED_CAST")
val connectionsProperty = (RealConnectionPool::class.memberProperties
        .find { it.name == "connections" }!!
        as KProperty1<RealConnectionPool, Deque<RealConnection>>).apply {
    isAccessible = true
}

fun ConnectionPool.listConnections(): Collection<RealConnection> {
    val delegate = delegateProperty.get(this)

    return connectionsProperty.get(delegate)
}

// Call

val realCallClass: KClass<Call> = Class.forName("okhttp3.RealCall").kotlin as KClass<Call>

@Suppress("UNCHECKED_CAST")
val transmitterProperty = realCallClass
        .memberProperties.find { it.name == "transmitter" }!!.apply {
    isAccessible = true
}

val transmitterClass: KClass<Transmitter> = Class.forName("okhttp3.internal.connection.Transmitter").kotlin as KClass<Transmitter>

@Suppress("UNCHECKED_CAST")
val listenerProperty = transmitterClass
        .memberProperties.find { it.name == "eventListener" }!!.apply {
    isAccessible = true
}

val Call.eventListener: EventListener
    get() {
        val transmitter = transmitterProperty.get(this) as Transmitter
        return listenerProperty.get(transmitter) as EventListener
    }

// Real Connection

val realConnectionClass: KClass<RealConnection> = Class.forName("okhttp3.internal.connection.RealConnection").kotlin as KClass<RealConnection>

@Suppress("UNCHECKED_CAST")
val noNewExchangesProperty = realConnectionClass
        .memberProperties.find { it.name == "noNewExchanges" }!!.apply {
    isAccessible = true
}

val RealConnection.noNewExchangesF
    get() = noNewExchangesProperty.get(this) as Boolean

@Suppress("UNCHECKED_CAST")
val successCountProperty = realConnectionClass
        .memberProperties.find { it.name == "successCount" }!!.apply {
    isAccessible = true
}

val RealConnection.successCountF
    get() = successCountProperty.get(this) as Int
