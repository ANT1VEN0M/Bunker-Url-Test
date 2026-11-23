package com.bunker.urltest

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

object UrlChecker {
    private val builtInTargets: List<Triple<String, String, String>> = listOf(
        Triple("google", "Google", "https://www.google.com/generate_204"),
        Triple("telegram", "Telegram", "https://telegram.org/"),
        Triple("instagram", "Instagram", "https://www.instagram.com/"),
        Triple("youtube", "YouTube", "https://m.youtube.com/generate_204"),
        Triple("discord", "Discord", "https://discord.com/"),
        Triple("x_twitter", "X / Twitter", "https://x.com/"),
        Triple("cloudflare", "Cloudflare", "https://1.1.1.1/"),
        Triple("chatgpt", "ChatGPT", "https://chatgpt.com/"),
        Triple("spotify", "Spotify", "https://www.spotify.com/"),
        Triple("steam", "Steam", "https://store.steampowered.com/")
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
        val timeoutMs = 3500
        var conn: HttpURLConnection? = null
        return try {
            val startTime = System.nanoTime()
            val url = URL(t.third)
            conn = url.openConnection() as HttpURLConnection
            val method = if (t.first == "youtube" || t.first == "google") "HEAD" else "GET"
            conn.requestMethod = method
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "BUNKER-URL-TEST/1.1")

            val responseCode = conn.responseCode
            val latencyMs = (System.nanoTime() - startTime) / 1_000_000
            CheckResult(t.first, t.second, latencyMs, Integer.valueOf(responseCode))
        } catch (_: Exception) {
            CheckResult(t.first, t.second, null, null)
        } finally {
            conn?.disconnect()
        }
    }
}
