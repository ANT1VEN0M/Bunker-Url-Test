package com.bunker.urltest

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object CustomServiceStore {
    private const val PREFS = "custom_services"
    private const val KEY_ITEMS = "items"
    private const val KEY_HIDDEN_BUILT_INS = "hidden_built_ins"

    fun hiddenBuiltIns(context: Context): Set<String> {
        val stringSet = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_HIDDEN_BUILT_INS, emptySet())
        return stringSet?.toSet() ?: emptySet()
    }

    fun hideBuiltIn(context: Context, id: String) {
        val hidden = hiddenBuiltIns(context).toMutableSet()
        hidden.add(id)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_HIDDEN_BUILT_INS, hidden)
            .apply()
    }

    fun restoreBuiltIns(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .remove(KEY_HIDDEN_BUILT_INS)
            .apply()
        context.getSharedPreferences("custom_service", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun replaceBuiltIn(context: Context, builtInId: String, name: String, url: String): Boolean {
        if (name.isBlank() || !isValidUrl(url)) return false
        hideBuiltIn(context, builtInId)
        add(context, name, url)
        return true
    }

    fun getAll(context: Context): List<CustomService> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, null) ?: return emptyList()
        val list = mutableListOf<CustomService>()
        return try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CustomService(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        url = obj.getString("url")
                    )
                )
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun add(context: Context, name: String, url: String): CustomService? {
        if (name.isBlank() || !isValidUrl(url)) return null
        val service = CustomService(
            id = UUID.randomUUID().toString().take(8),
            name = name.trim(),
            url = normalizeUrl(url)
        )
        val list = getAll(context).toMutableList()
        list.add(service)
        save(context, list)
        return service
    }

    fun update(context: Context, id: String, name: String, url: String): Boolean {
        if (name.isBlank() || !isValidUrl(url)) return false
        val list = getAll(context).map {
            if (it.id == id) it.copy(name = name.trim(), url = normalizeUrl(url)) else it
        }
        save(context, list)
        return true
    }

    fun remove(context: Context, id: String) {
        val list = getAll(context).filterNot { it.id == id }
        save(context, list)
    }

    private fun save(context: Context, services: List<CustomService>) {
        val arr = JSONArray()
        services.forEach { service ->
            arr.put(
                JSONObject()
                    .put("id", service.id)
                    .put("name", service.name)
                    .put("url", service.url)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, arr.toString())
            .apply()
    }

    fun isValidUrl(value: String): Boolean {
        return try {
            val uri = Uri.parse(normalizeUrl(value))
            val scheme = uri.scheme
            (scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()
        } catch (_: Throwable) {
            false
        }
    }

    fun normalizeUrl(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
