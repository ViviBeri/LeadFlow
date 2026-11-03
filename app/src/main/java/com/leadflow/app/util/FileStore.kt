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
     * Publishes complete files only. Existing exports are retained until replacements
     * have been written successfully.
     */
    fun save(context: Context, fileName: String, mime: String, bytes: ByteArray): SavedExport {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("Unable to create export directory")
        val shareFile = File(dir, fileName)
        val temporary = File(dir, ".$fileName.tmp")
        try {
            temporary.writeBytes(bytes)
            if (!temporary.renameTo(shareFile)) {
                throw IllegalStateException("Unable to publish share export")
            }
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }

        val inDownloads = runCatching {
            copyToDownloads(context, fileName, mime, bytes)
        }.getOrDefault(false)
        return SavedExport(shareFile, inDownloads)
    }

    private fun copyToDownloads(context: Context, fileName: String, mime: String, bytes: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val relativePath = Environment.DIRECTORY_DOWNLOADS + "/LeadFlow/"
            val previousIds = mutableListOf<Long>()

            resolver.query(
                collection,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
                arrayOf(fileName, relativePath),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) previousIds.add(cursor.getLong(0))
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return false
            val published = runCatching {
                val output = resolver.openOutputStream(uri) ?: error("Unable to open Downloads output")
                output.use { it.write(bytes) }
                val publish = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                check(resolver.update(uri, publish, null, null) == 1) {
                    "Unable to publish Downloads export"
                }
            }.isSuccess

            if (!published) {
                resolver.delete(uri, null, null)
                return false
            }

            previousIds.forEach { id ->
                runCatching {
                    resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
                }
            }
            val restoreName = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            }
            val renamed = runCatching {
                resolver.update(uri, restoreName, null, null) == 1
            }.getOrDefault(false)
            if (!renamed) {
                runCatching { resolver.delete(uri, null, null) }
                return false
            }
            return true
        }

        @Suppress("DEPRECATION")
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "LeadFlow"
        )
        if (!directory.exists() && !directory.mkdirs()) return false

        val target = File(directory, fileName)
        val temporary = File(directory, ".$fileName.tmp")
        return try {
            temporary.writeBytes(bytes)
            if (!temporary.renameTo(target)) error("Unable to publish Downloads export")
            true
        } catch (error: Exception) {
            temporary.delete()
            false
        }
    }
}
