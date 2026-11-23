package com.bunker.urltest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object AppNetworkChecker {
    private val profiles: Map<String, List<String>> = mapOf(
        "org.telegram.messenger" to listOf("telegram.org", "api.telegram.org", "t.me"),
        "org.telegram.messenger.web" to listOf("telegram.org", "api.telegram.org", "t.me"),
        "org.thunderdog.challegram" to listOf("telegram.org", "api.telegram.org", "t.me"),
        "com.google.android.youtube" to listOf("youtube.com", "youtubei.googleapis.com", "googlevideo.com"),
        "com.instagram.android" to listOf("instagram.com", "i.instagram.com", "cdninstagram.com"),
        "com.whatsapp" to listOf("whatsapp.com", "web.whatsapp.com", "mmg.whatsapp.net"),
        "com.discord" to listOf("discord.com", "gateway.discord.gg", "cdn.discordapp.com"),
        "com.zhiliaoapp.musically" to listOf("tiktok.com", "www.tiktok.com", "tiktokcdn.com"),
        "com.ss.android.ugc.trill" to listOf("tiktok.com", "www.tiktok.com", "tiktokcdn.com"),
        "com.spotify.music" to listOf("spotify.com", "api.spotify.com", "audio-ak-spotify-com.akamaized.net"),
        "com.twitter.android" to listOf("x.com", "api.x.com", "pbs.twimg.com"),
        "com.twitter.android.lite" to listOf("x.com", "api.x.com", "pbs.twimg.com"),
        "com.openai.chatgpt" to listOf("chatgpt.com", "api.openai.com", "oaistatic.com"),
        "com.valvesoftware.android.steam.community" to listOf("store.steampowered.com", "steamcommunity.com"),
        "com.google.android.gm" to listOf("google.com", "mail.google.com", "gmail.com"),
        "com.google.android.apps.maps" to listOf("google.com", "maps.google.com", "maps.googleapis.com"),
        "com.viber.voip" to listOf("viber.com", "media.cdn.viber.com"),
        "com.snapchat.android" to listOf("snapchat.com", "app.snapchat.com", "sc-cdn.net"),
        "com.reddit.frontpage" to listOf("reddit.com", "www.reddit.com", "v.redd.it"),
        "com.facebook.katana" to listOf("facebook.com", "graph.facebook.com", "fbcdn.net"),
        "com.facebook.orca" to listOf("facebook.com", "graph.facebook.com", "fbcdn.net"),
        "com.google.android.apps.messaging" to listOf("messages.google.com", "google.com"),
        "com.linkedin.android" to listOf("linkedin.com", "licdn.com"),
        "com.pinterest" to listOf("pinterest.com", "i.pinimg.com"),
        "com.tumblr" to listOf("tumblr.com", "www.tumblr.com"),
        "com.skype.raider" to listOf("skype.com", "login.skype.com"),
        "com.microsoft.teams" to listOf("teams.microsoft.com", "microsoft.com"),
        "com.microsoft.office.outlook" to listOf("outlook.com", "office.com", "microsoft.com"),
        "com.amazon.mShop.android.shopping" to listOf("amazon.com", "amazonaws.com"),
        "com.ebay.mobile" to listOf("ebay.com", "ebaystatic.com"),
        "com.netflix.mediaclient" to listOf("netflix.com", "nflxvideo.net", "nflximg.net"),
        "com.google.android.apps.photos" to listOf("photos.google.com", "google.com"),
        "com.google.android.googlequicksearchbox" to listOf("google.com", "gstatic.com"),
        "com.yandex.browser" to listOf("yandex.ru", "yandex.net", "yastatic.net"),
        "ru.yandex.searchplugin" to listOf("yandex.ru", "yandex.net", "yastatic.net"),
        "ru.vk.android" to listOf("vk.com", "userapi.com", "vkuser.net"),
        "com.vkontakte.android" to listOf("vk.com", "userapi.com", "vkuser.net"),
        "com.ok.android" to listOf("ok.ru", "odnoklassniki.ru"),
        "ru.mail.mailapp" to listOf("mail.ru", "imgsmail.ru"),
        "ru.ozon.app.android" to listOf("ozon.ru", "ozonusercontent.com"),
        "ru.wildberries" to listOf("wildberries.ru", "wb.ru"),
        "com.wildberries.wb" to listOf("wildberries.ru", "wb.ru"),
        "ru.tinkoff.android" to listOf("tbank.ru", "tinkoff.ru"),
        "com.idamob.tinkoff.android" to listOf("tbank.ru", "tinkoff.ru"),
        "ru.sberbankmobile" to listOf("sberbank.ru", "sber.ru"),
        "com.avito.android" to listOf("avito.ru", "avito.st"),
        "com.duolingo" to listOf("duolingo.com", "duolingo.cn"),
        "com.ubercab" to listOf("uber.com", "uber-assets.com"),
        "com.grabtaxi.passenger" to listOf("grab.com", "grabtaxi.com"),
        "com.alibaba.aliexpresshd" to listOf("aliexpress.com", "alicdn.com"),
        "com.notion.id" to listOf("notion.so"),
        "com.github.android" to listOf("github.com", "api.github.com"),
        "ru.rutube.app" to listOf("rutube.ru"),
        "ru.kinopoisk" to listOf("kinopoisk.ru"),
        "com.cloudflare.onedotonedotonedotone" to listOf("cloudflare-dns.com", "one.one.one.one")
    )

    fun serviceCandidates(packageName: String, appLabel: String = ""): Pair<List<String>, Boolean> {
        val knownHosts = profiles[packageName]
        val estimated = knownHosts == null
        val hosts = knownHosts ?: guessHosts(packageName, appLabel)
        return Pair(hosts.distinct(), estimated)
    }

    suspend fun check(packageName: String, appLabel: String = ""): AppNetworkResult = withContext(Dispatchers.IO) {
        val (hosts, isEstimated) = serviceCandidates(packageName, appLabel)
        if (hosts.isEmpty()) {
            return@withContext AppNetworkResult(
                packageName = packageName,
                appLabel = appLabel.ifBlank { packageName },
                hosts = emptyList(),
                availableHosts = emptyList(),
                averageLatencyMs = null,
                isEstimated = isEstimated
            )
        }

        val results = hosts.map { host ->
            async {
                val (ok, latency) = checkHost(host)
                Triple(host, ok, latency)
            }
        }.awaitAll()

        val available = results.filter { it.second }.map { it.first }
        val latencies = results.mapNotNull { it.third }
        val avgLatency = if (latencies.isNotEmpty()) latencies.average().toLong() else null

        AppNetworkResult(
            packageName = packageName,
            appLabel = appLabel.ifBlank { packageName },
            hosts = hosts,
            availableHosts = available,
            averageLatencyMs = avgLatency,
            isEstimated = isEstimated
        )
    }

    private suspend fun checkHost(host: String): Pair<Boolean, Long?> = withContext(Dispatchers.IO) {
        val startTime = System.nanoTime()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://$host/")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "BUNKER-URL-TEST/1.1")
            conn.setRequestProperty("Range", "bytes=0-0")
            conn.responseCode
            val latency = (System.nanoTime() - startTime) / 1_000_000
            Pair(true, latency)
        } catch (_: Throwable) {
            Pair(false, null)
        } finally {
            conn?.disconnect()
        }
    }

    private fun guessHosts(packageName: String, appLabel: String): List<String> {
        val candidates = mutableListOf<String>()
        val parts = packageName.split(".").filter { it.length > 2 && it !in setOf("com", "ru", "org", "net", "app", "android", "apps") }
        if (parts.isNotEmpty()) {
            val base = parts.last().lowercase(Locale.ROOT)
            candidates.add("$base.com")
            candidates.add("$base.ru")
        }
        val cleanLabel = appLabel.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        if (cleanLabel.length > 2) {
            candidates.add("$cleanLabel.com")
            candidates.add("$cleanLabel.ru")
        }
        return candidates.distinct()
    }
}
