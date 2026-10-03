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

            // Remove an older copy so we don't end up with "leads (1).csv".
            resolver.query(
                collection,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
                arrayOf(fileName, "$relPath/"),
                null
            )?.use { c ->
                while (c.moveToNext()) {
                    resolver.delete(ContentUris.withAppendedId(collection, c.getLong(0)), null, null)
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, relPath)
            }
            val uri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
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
