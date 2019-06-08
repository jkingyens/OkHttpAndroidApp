package com.okhttpandroidapp.factory

import okhttp3.CookieJar

class Config(
        val optimised: Boolean = true,
        val useCache: Boolean = true,
        val ctHosts: List<String> = listOf(),
        val cookieJar: CookieJar? = null,
        val conscrypt: Boolean = true)