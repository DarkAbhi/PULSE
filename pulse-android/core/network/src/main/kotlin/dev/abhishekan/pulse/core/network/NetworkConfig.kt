package dev.abhishekan.pulse.core.network

import java.net.URI

class NetworkConfig(val baseUrl: String, val debug: Boolean) {
    init {
        val uri = URI(baseUrl)
        require(uri.host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
        require(uri.path == "/api/") { "API address must end in /api/" }
        require(uri.scheme == "https" || (debug && uri.scheme == "http" && uri.host == "10.0.2.2")) {
            "HTTPS is required except for the debug emulator host"
        }
    }
}
