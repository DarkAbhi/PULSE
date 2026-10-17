package dev.abhishekan.pulse.core.data

import android.content.Context
import androidx.core.net.toUri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import dev.abhishekan.pulse.core.domain.model.RECEIPT_LIMIT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class ReceiptFileException : Exception("Choose a readable file between 1 byte and 10 MB")
data class ReceiptBytes(val name: String, val type: String, val bytes: ByteArray)
interface ReceiptFiles {
    suspend fun read(source: String): ReceiptBytes
    suspend fun write(bytes: ByteArray, name: String, destination: String?): String
}
class AndroidReceiptFiles(private val context: Context) : ReceiptFiles {
    override suspend fun read(source: String): ReceiptBytes = try { withContext(Dispatchers.IO) {
        val uri = source.toUri()
        if (uri.scheme != "content") throw ReceiptFileException()
        var name = "receipt"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let { if (cursor.getLong(it) > RECEIPT_LIMIT) throw ReceiptFileException() }
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
            }
        }
        name = safeReceiptName(name)
        val type = context.contentResolver.getType(uri)?.takeIf { it.matches(Regex("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+")) } ?: "application/octet-stream"
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                if (output.size() + count > RECEIPT_LIMIT) throw ReceiptFileException()
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw ReceiptFileException()
        if (bytes.isEmpty()) throw ReceiptFileException()
        ReceiptBytes(name, type, bytes)
    } } catch (error: CancellationException) { throw error }
    catch (_: java.io.IOException) { throw ReceiptFileException() }
    catch (_: SecurityException) { throw ReceiptFileException() }
    override suspend fun write(bytes: ByteArray, name: String, destination: String?): String = withContext(Dispatchers.IO) {
        require(bytes.size in 1..RECEIPT_LIMIT)
        if (destination != null) {
            val uri = destination.toUri()
            if (uri.scheme != "content") throw ReceiptFileException()
            (context.contentResolver.openOutputStream(uri, "wt") ?: throw ReceiptFileException()).use { ensureActive(); it.write(bytes) }
            destination
        } else {
            val directory = File(context.cacheDir, "receipts").apply { mkdirs() }
            // Keep recent shared previews available; clear older cache entries on subsequent opens.
            directory.listFiles().orEmpty().filter { it.lastModified() < System.currentTimeMillis() - 86_400_000 }.forEach { it.deleteRecursively() }
            val folder = File(directory, UUID.randomUUID().toString()).apply { mkdirs() }
            val file = File(folder, safeReceiptName(name))
            try { file.writeBytes(bytes); FileProvider.getUriForFile(context, "${context.packageName}.receipts", file).toString() }
            catch (error: Exception) { folder.deleteRecursively(); throw error }
        }
    }
}
internal fun safeReceiptName(value: String): String {
    val clean = value.substringAfterLast('/').substringAfterLast('\\').filter { it.code >= 32 && it.code != 127 && it !in "\";\r\n" }
        .takeUnless { it.isBlank() || it == "." || it == ".." } ?: "receipt"
    // Sixty Unicode code points fit the backend's 255-byte name limit without splitting an emoji.
    return clean.take(clean.offsetByCodePoints(0, minOf(60, clean.codePointCount(0, clean.length))))
}
