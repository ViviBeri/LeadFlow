package com.leadflow.app.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AtomicFile
import java.io.File

data class SavedExport(val shareFile: File, val inDownloads: Boolean)

object FileStore {
    /**
     * Writes a private share copy and then publishes a separate copy in Downloads.
     * The private copy is committed atomically so a failed write cannot corrupt
     * the previous shareable export.
     */
    fun save(context: Context, fileName: String, mime: String, bytes: ByteArray): SavedExport {
        val dir = File(context.filesDir, "exports").apply {
            if (!exists() && !mkdirs()) error("Unable to create private export directory")
        }
        val shareFile = File(dir, fileName)
        val atomicFile = AtomicFile(shareFile)
        val stream = atomicFile.startWrite()
        try {
            stream.write(bytes)
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
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
                val output = resolver.openOutputStream(uri) ?: error("Unable to open export output stream")
                output.use { it.write(bytes) }
                val publish = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, publish, null, null) > 0
            }.getOrDefault(false)

            if (!written) {
                resolver.delete(uri, null, null)
                return false
            }

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
        return replaceFileAtomically(File(dir, fileName), bytes)
    }

    /**
     * Replaces a legacy Downloads file through a same-directory temporary file.
     * The previous export is retained if writing or committing the replacement fails.
     */
    private fun replaceFileAtomically(target: File, bytes: ByteArray): Boolean {
        val temp = File(target.parentFile, ".${target.name}.tmp")
        val backup = File(target.parentFile, ".${target.name}.bak")
        return try {
            temp.outputStream().use { output ->
                output.write(bytes)
                output.flush()
                (output as? java.io.FileOutputStream)?.fd?.sync()
            }
            if (backup.exists() && !backup.delete()) error("Unable to clear previous export backup")
            if (target.exists() && !target.renameTo(backup)) error("Unable to preserve previous export")
            if (!temp.renameTo(target)) {
                if (backup.exists()) backup.renameTo(target)
                error("Unable to publish replacement export")
            }
            backup.delete()
            true
        } catch (_: Exception) {
            temp.delete()
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            false
        }
    }
}
