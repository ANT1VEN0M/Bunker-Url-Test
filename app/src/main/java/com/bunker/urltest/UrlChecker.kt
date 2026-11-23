package com.bunker.urltest

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

object UrlChecker {
    private val builtInTargets: List<Triple<String, String, String>> = listOf(
        Triple("google", "Google", "https://www.google.com/generate_204"),
        Triple("telegram", "Telegram", "https://t.me"),
        Triple("instagram", "Instagram", "https://www.instagram.com/"),
        Triple("youtube", "YouTube", "https://www.youtube.com/generate_204")
    )

    fun targets(context: Context): List<Triple<String, String, String>> {
        val custom = CustomServiceStore.getAll(context).map {
            Triple("custom_${it.id}", it.name, it.url)
        }
        val hidden = CustomServiceStore.hiddenBuiltIns(context)
        val visibleBuiltIns = builtInTargets.filterNot { hidden.contains(it.first) }
        return visibleBuiltIns + custom
    }

    fun check(t: Triple<String, String, String>): CheckResult {
        val timeoutMs = 2500
        var conn: HttpURLConnection? = null
        return try {
            val startTime = System.nanoTime()
            val url = URL(t.third)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            conn.setRequestProperty("Connection", "close")
            conn.setRequestProperty("Accept", "*/*")

            val responseCode = conn.responseCode
            val latencyMs = (System.nanoTime() - startTime) / 1_000_000
            CheckResult(t.first, t.second, latencyMs, responseCode)
        } catch (_: Exception) {
            CheckResult(t.first, t.second, null, null)
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {
            }
        }
    }
}
