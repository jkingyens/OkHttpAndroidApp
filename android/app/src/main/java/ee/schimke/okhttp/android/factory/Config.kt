package ee.schimke.okhttp.android.factory

class Config(
        val useCache: Boolean = true,
        val ctHosts: List<String> = listOf(),
        val conscrypt: Boolean = true,
        val cacheSize: Long = 1024 * 1024 * 64L,
        val quicHosts: List<String> = listOf(),
        val closeInBackground: Boolean = true,
        val warmedConnections: List<String> = listOf(),
        val doh: Boolean = true,
        val networkSelector: NetworkSelector = Default)