package com.okhttpandroidapp.factory

import android.util.Log
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

class SmartSocketFactory(private val androidNetworkManager: AndroidNetworkManager) : SocketFactory() {
    private val systemFactory = getDefault()

    private val networkSocketMap = mutableMapOf<String, MutableList<Socket>>()

    override fun createSocket(): Socket {
        val s = systemFactory.createSocket()

        Log.i("SmartSocketFactory", "createSocket")

        val selectedSocketAddress = androidNetworkManager.selectLocalSocketAddress()

        if (selectedSocketAddress != null) {
            s.bind(selectedSocketAddress)
        }

        return s
    }

    override fun createSocket(host: String?, port: Int): Socket {
        TODO("okhttp no likey")
    }

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        TODO("okhttp no likey")
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        TODO("okhttp no likey")
    }

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        TODO("okhttp no likey")
    }
}