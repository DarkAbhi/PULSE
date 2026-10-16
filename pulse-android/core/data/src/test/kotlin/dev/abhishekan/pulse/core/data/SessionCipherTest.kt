package dev.abhishekan.pulse.core.data

import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import org.junit.Assert.*
import org.junit.Test

class SessionCipherTest {
    private fun cipher() = SessionCipher(KeyGenerator.getInstance("AES").apply { init(256) }.generateKey())

    @Test fun ciphertextIsRandomizedAndRoundTrips() {
        val cipher = cipher()
        val secret = "session-secret".toByteArray()
        val first = cipher.encrypt(secret)
        val second = cipher.encrypt(secret)
        assertFalse(first.contentEquals(second))
        assertFalse(String(first).contains("session-secret"))
        assertArrayEquals(secret, cipher.decrypt(first))
    }

    @Test fun tamperingAndWrongKeysAreRejected() {
        val cipher = cipher()
        val encrypted = cipher.encrypt("secret".toByteArray())
        assertThrows(AEADBadTagException::class.java) { cipher().decrypt(encrypted) }
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(encrypted) }
    }

    @Test fun invalidEnvelopesAreRejected() {
        val cipher = cipher()
        assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(byteArrayOf()) }
        val version = cipher.encrypt(byteArrayOf()).apply { this[0] = 2 }
        assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(version) }
    }
}
