package com.leadflow.app.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.util.UUID

data class SavedExport(val shareFile: File, val inDownloads: Boolean)

object FileStore {
    /**
     * Saves a complete private share file, then attempts to publish a Downloads copy.
     * A failed Downloads save does not invalidate the private share file.
     */
    fun save(context: Context, fileName: String, mime: String, bytes: ByteArray): SavedExport {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("Unable to create export directory")
        val shareFile = File(dir, fileName)
        val temporary = File(dir, ".$fileName.${UUID.randomUUID()}.tmp")
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

    /**
     * Publishes a Downloads copy and removes older exact-name copies only after
     * confirming that MediaStore kept the requested filename.
     */
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

            val stagedPreviousIds = mutableListOf<Long>()
            val stagedPreviousNames = mutableMapOf<Long, String>()

            fun restorePreviousRows() {
                stagedPreviousIds.forEach { id ->
                    runCatching {
                        val restore = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        }
                        resolver.update(ContentUris.withAppendedId(collection, id), restore, null, null)
                    }
                }
                stagedPreviousIds.clear()
            }

            // Android 10 does not reliably allow inserting a second row with the
            // same Downloads name while the old row still occupies that path.
            // Temporarily rename old rows, then restore them if replacement fails.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                for (id in previousIds) {
                    val backupName = ".$fileName.${UUID.randomUUID()}.backup"
                    val rename = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, backupName)
                    }
                    val renamed = runCatching {
                        resolver.update(ContentUris.withAppendedId(collection, id), rename, null, null) == 1
                    }.getOrDefault(false)
                    if (!renamed) {
                        restorePreviousRows()
                        return false
                    }
                    stagedPreviousIds.add(id)
                    stagedPreviousNames[id] = backupName
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = runCatching { resolver.insert(collection, values) }.getOrNull()
            if (uri == null) {
                restorePreviousRows()
                return false
            }
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
                runCatching { resolver.delete(uri, null, null) }
                restorePreviousRows()
                return false
            }

            val restoreName = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            }
            val renamed = runCatching {
                resolver.update(uri, restoreName, null, null) == 1
            }.getOrDefault(false)

            if (!renamed) {
                // Keep the published file and restore the previous export names.
                restorePreviousRows()
                return false
            }

            val effectiveName = runCatching {
                resolver.query(
                    uri,
                    arrayOf(MediaStore.Downloads.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()

            if (effectiveName != fileName) {
                // Preserve older exports because the requested name was not obtained.
                restorePreviousRows()
                return false
            }

            val exactExportSelection =
                "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"

            previousIds.forEach { id ->
                // Android 10 rows are still under their unique staging names here.
                // On newer Android versions, the expected name remains fileName.
                val expectedName = stagedPreviousNames[id] ?: fileName
                val expectedArgs = arrayOf(expectedName, relativePath)
                val previousUri = ContentUris.withAppendedId(collection, id)
                val stillMatches = resolver.query(
                    previousUri,
                    arrayOf(
                        MediaStore.Downloads.DISPLAY_NAME,
                        MediaStore.Downloads.RELATIVE_PATH
                    ),
                    exactExportSelection,
                    expectedArgs,
                    null
                )?.use { cursor ->
                    cursor.moveToFirst() &&
                        cursor.getString(0) == expectedName &&
                        cursor.getString(1) == relativePath
                } == true

                // If a user or another app renamed/moved the old file, leave it alone.
                if (stillMatches) {
                    check(
                        resolver.delete(previousUri, exactExportSelection, expectedArgs) == 1
                    ) { "Unable to remove the replaced Downloads export" }
                }
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
        val temporary = File(directory, ".$fileName.${UUID.randomUUID()}.tmp")
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
