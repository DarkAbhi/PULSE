package dev.abhishekan.pulse.core.data

import android.content.Context
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AndroidSessionKey {
    @Synchronized
    fun get(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(Alias, null) as? SecretKey)?.let { return it }
        if (!create) throw IOException("Session key unavailable")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }
    private companion object { const val Alias = "pulse.session.v1" }
}

internal class SessionCipher(private val key: SecretKey) {
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(Aad)
        check(cipher.iv.size == 12)
        return byteArrayOf(1) + cipher.iv + cipher.doFinal(plaintext)
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        require(bytes.size >= 29 && bytes[0] == 1.toByte()) { "Invalid session envelope" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD(Aad)
        return cipher.doFinal(bytes, 13, bytes.size - 13)
    }
    private companion object { val Aad = "dev.abhishekan.pulse/session/v1".toByteArray() }
}

/** Encrypted backing store for the session cookie envelope. */
class EncryptedSessionStore(context: Context, private val key: AndroidSessionKey) : SessionMaterialStore {
    private val file = AtomicFile(java.io.File(context.noBackupFilesDir, "session.enc"))
    private val mutex = Mutex()

    override suspend fun read(): ByteArray? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!file.baseFile.exists()) null
            else SessionCipher(key.get(create = false)).decrypt(file.readFully())
        }
    }

    override suspend fun write(material: ByteArray) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val encrypted = SessionCipher(key.get(create = true)).encrypt(material)
            val stream = file.startWrite()
            try {
                stream.write(encrypted)
                file.finishWrite(stream)
            } catch (error: Throwable) {
                file.failWrite(stream)
                throw error
            }
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) { mutex.withLock { file.delete() } }
}
