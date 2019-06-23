# OkHttpAndroidApp
An Android App for testing OkHttp

An Anatomically Correct Network Manager for OkHttp
- Soft closes on network changes (airplane mode)
- DNS, Proxy selected based on Network rules - not blended
- API for selecting network to use for each connection (intranet -> Wifi/VPN, external -> Cell First, video -> Wifi only)

Possible
- Corporate defaults e.g. App only available on (BigCorp Wifi)
- Ship smart defaults e.g. Cellular for API usage, Wifi preferred for downloads.
- Set handling for background/foreground connections
- Batch network operations within app

<img width="330" alt="image" src="https://user-images.githubusercontent.com/231923/59976438-76bd0480-95bc-11e9-8f59-395e2506df3e.png">

TODO

- Correct/Complete network selection/detection
- Wire up client selection strategies
- Allow overriding e.g. forcing a specific connection without disconnecting
- Understand Multipath TCP (http://amiusingmptcp.de/)
- Show clean usage of stats e.g. how would you monitor this
- publish a separate library
- test with live different proxies
- Support down to Android 21+
- Switch address to force reconnects using SocketFactory etc?
- Airplane mode
- Hard close/prune connections
- keep connection open in foreground, close on background
- DNS over HTTPS
- https://github.com/facebookincubator/Battery-Metrics

Experiments

https://github.com/MPTCP-smartphone-thesis/MultipathControl/releases

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "Wifi Wakelock");
        wifiLock.acquire();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        partialLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "3G Wakelock");
        partialLock.acquire();

        connectivityManager.requestRouteToHost(
ConnectivityManager.TYPE_MOBILE_HIPRI, hostAddress);

        if (isMobileDataEnabled() && isWifiConnected() && mEnabled)
			cManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
"enableHIPRI");
