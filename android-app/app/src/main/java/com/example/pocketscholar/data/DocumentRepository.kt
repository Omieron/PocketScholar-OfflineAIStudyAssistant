package com.example.pocketscholar.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val PREFS_NAME = "pocketscholar_documents"
private const val KEY_DOCUMENTS = "documents"

class DocumentRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDocuments(): List<Document> {
        val json = prefs.getString(KEY_DOCUMENTS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                Document(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    path = obj.optString("path", "")
                ).takeIf { it.id.isNotEmpty() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addDocument(doc: Document) {
        val list = getDocuments().toMutableList()
        if (list.any { it.id == doc.id }) return
        list.add(doc)
        save(list)
    }

    fun removeDocument(id: String) {
        val list = getDocuments().filter { it.id != id }
        getDocuments().firstOrNull { it.id == id }?.path?.let { path ->
            try { File(path).delete() } catch (_: Exception) { }
        }
        save(list)
    }

    private fun save(list: List<Document>) {
        val arr = JSONArray()
        list.forEach { doc ->
            arr.put(JSONObject().apply {
                put("id", doc.id)
                put("name", doc.name)
                put("path", doc.path)
            })
        }
        prefs.edit().putString(KEY_DOCUMENTS, arr.toString()).apply()
    }
}
