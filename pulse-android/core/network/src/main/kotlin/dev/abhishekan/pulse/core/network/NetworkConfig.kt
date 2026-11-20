package dev.abhishekan.pulse.core.network

import java.net.URI

class NetworkConfig(
    val baseUrl: String,
    val debug: Boolean,
) {
    init {
        val uri = URI(baseUrl)
        require(
            uri.host != null &&
                uri.rawUserInfo == null &&
                uri.rawQuery == null &&
                uri.rawFragment == null,
        )
        require(uri.path == "/api/") { "API address must end in /api/" }
        require(
            uri.scheme == "https" ||
                (
                    uri.scheme == "http" &&
                        (
                            (uri.host == "100.115.134.90" && uri.port == 3100) ||
                                (debug && uri.host == "10.0.2.2")
                        )
                ),
        ) {
            "HTTP is allowed only for the Pulse Tailscale endpoint or the debug emulator host"
        }
    }
}
