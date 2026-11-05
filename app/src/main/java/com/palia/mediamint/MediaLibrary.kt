package com.palia.mediamint

import android.content.Context
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LibraryFile(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val path: String,
    val sizeBytes: Long
) {
    val file: File get() = File(path)
}

object MediaLibrary {
    private const val PREFS = "palia_media_library"
    private const val KEY_ITEMS = "items"

    fun save(context: Context, media: ProcessedMedia): LibraryFile {
        val dir = File(context.filesDir, "converted_media").apply { mkdirs() }
        val safeName = media.fileName.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val target = File(dir, "${System.currentTimeMillis()}_$safeName")
        target.writeBytes(media.bytes)
        exportToPhoneFolder(context, safeName, media.mimeType, media.bytes)
        val item = LibraryFile(target.name, safeName, media.mimeType, target.absolutePath, target.length())
        val all = list(context).filterNot { it.path == item.path } + item
        persist(context, all)
        return item
    }

    fun list(context: Context): List<LibraryFile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]") ?: "[]"
        val result = mutableListOf<LibraryFile>()
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val item = LibraryFile(o.getString("id"), o.getString("name"), o.getString("mime"), o.getString("path"), o.getLong("size"))
            if (item.file.exists()) result += item
        }
        if (result.size != array.length()) persist(context, result)
        return result.sortedByDescending { it.file.lastModified() }
    }

    private fun exportToPhoneFolder(context: Context, name: String, mime: String, bytes: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val category = when {
                mime.startsWith("audio/") -> "Audio"
                mime.startsWith("video/") -> "Video"
                mime.startsWith("image/") -> "Images"
                mime == "application/pdf" -> "PDF"
                else -> "Other"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MediaMint/" + category)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values) ?: return
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Cannot write exported file")
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                context.contentResolver.update(uri, done, null, null)
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
        }
    }

    private fun persist(context: Context, items: List<LibraryFile>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("name", item.fileName)
                .put("mime", item.mimeType).put("path", item.path).put("size", item.sizeBytes))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, array.toString()).apply()
    }
}
