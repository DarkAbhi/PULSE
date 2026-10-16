package dev.abhishekan.pulse.core.domain.model

enum class Failure {
    InvalidInput, InvalidCredentials, Unauthorized, Connection, Timeout, Server, Malformed, Storage,
}
