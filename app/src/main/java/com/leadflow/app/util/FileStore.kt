package com.leadflow.app.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class SavedExport(val shareFile: File, val inDownloads: Boolean)

object FileStore {
    /**
     * 1) Writes the file to the app's private "exports" folder (this copy is what FileProvider shares).
     * 2) Copies it to Downloads/LeadFlow/ so the user can find it in any file manager.
     */
    fun save(context: Context, fileName: String, mime: String, bytes: ByteArray): SavedExport {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val shareFile = File(dir, fileName).also { it.writeBytes(bytes) }
        val ok = runCatching { copyToDownloads(context, fileName, mime, bytes) }.getOrDefault(false)
        return SavedExport(shareFile, ok)
    }

    private fun copyToDownloads(context: Context, fileName: String, mime: String, bytes: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: MediaStore, no storage permission needed.
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val relPath = Environment.DIRECTORY_DOWNLOADS + "/LeadFlow"

            // Keep the previous export until the replacement has been written successfully.
            val previousCopies = mutableListOf<Uri>()
            resolver.query(
                collection,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
                arrayOf(fileName, "$relPath/"),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    previousCopies += ContentUris.withAppendedId(collection, cursor.getLong(0))
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, relPath)
            }
            val newUri = resolver.insert(collection, values) ?: return false
            val saved = runWithCleanupOnFailure(
                resource = newUri,
                action = {
                    resolver.openOutputStream(newUri)?.use { it.write(bytes) } != null
                },
                cleanup = { failedUri -> resolver.delete(failedUri, null, null) }
            )
            if (!saved) return false

            // Only replace older copies after the new export is complete.
            previousCopies.forEach { oldUri ->
                runCatching { resolver.delete(oldUri, null, null) }
            }
            return true
        } else {
            // Android 7-9: classic file access (needs WRITE_EXTERNAL_STORAGE granted).
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LeadFlow")
            dir.mkdirs()
            File(dir, fileName).writeBytes(bytes)
            return true
        }
    }
}
