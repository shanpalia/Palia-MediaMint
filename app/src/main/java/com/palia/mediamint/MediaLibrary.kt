package com.palia.mediamint

import android.content.Context
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

    private fun persist(context: Context, items: List<LibraryFile>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("name", item.fileName)
                .put("mime", item.mimeType).put("path", item.path).put("size", item.sizeBytes))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, array.toString()).apply()
    }
}
