package com.okhttpandroidapp.factory

import okhttp3.Dns
import java.net.InetAddress

class AndroidDns: Dns {
    override fun lookup(hostname: String): MutableList<InetAddress> {
        // TODO implement
        return Dns.SYSTEM.lookup(hostname)
    }
}