# OkHttpAndroidApp
An Android App for testing OkHttp

An Anatomically Correct Network Manager for OkHttp
- Soft closes on network changes (airplane mode)
- DNS, Proxy selected based on Network rules - not blended
- API for selecting network to use for each connection (intranet -> Wifi/VPN, external -> Cell First, video -> Wifi only)
- Corporate defaults e.g. App only available on (BigCorp Wifi)
- Ship smart defaults e.g. Cellular for API usage, Wifi preferred for downloads.

<img width="296" alt="image" src="https://user-images.githubusercontent.com/231923/59155710-99f99700-8a87-11e9-9096-1ee3e6da029f.png">

TODO

- Correct/Complete network selection/detection
- Wire up client selection strategies
- Allow overriding e.g. forcing a specific connection without disconnecting
- Understand multipath TCP
- Show clean usage of stats e.g. how would you monitor this
- publish a separate library
- test with live different proxies
