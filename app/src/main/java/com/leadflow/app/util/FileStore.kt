package com.leadflow.app.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class SavedExport(val shareFile: File, val inDownloads: Boolean)

object FileStore {
    /**
     * 1) Writes the file to the app's private exports folder (shared through FileProvider).
     * 2) Copies it to Downloads/LeadFlow so it is available in file managers.
     */
    fun save(context: Context, fileName: String, mime: String, bytes: ByteArray): SavedExport {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val shareFile = File(dir, fileName)
        try {
            shareFile.writeBytes(bytes)
        } catch (error: Exception) {
            shareFile.delete()
            throw error
        }
        val ok = runCatching { copyToDownloads(context, fileName, mime, bytes) }.getOrDefault(false)
        return SavedExport(shareFile, ok)
    }

    private fun copyToDownloads(context: Context, fileName: String, mime: String, bytes: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val relPath = Environment.DIRECTORY_DOWNLOADS + "/LeadFlow"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "$relPath/")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return false
            val written = runCatching {
                val stream = resolver.openOutputStream(uri) ?: error("Unable to open export output stream")
                stream.use { it.write(bytes) }
                val publish = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, publish, null, null) > 0
            }.getOrDefault(false)

            if (!written) {
                resolver.delete(uri, null, null)
                return false
            }

            // Replace older copies only after the new export has been fully written.
            resolver.query(
                collection,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads._ID}!=?",
                arrayOf(fileName, "$relPath/", ContentUris.parseId(uri).toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    resolver.delete(ContentUris.withAppendedId(collection, cursor.getLong(0)), null, null)
                }
            }
            return true
        }

        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LeadFlow")
        if (!dir.exists() && !dir.mkdirs()) return false
        val target = File(dir, fileName)
        return runCatching {
            target.writeBytes(bytes)
            true
        }.getOrElse {
            target.delete()
            false
        }
    }
}
