package com.bunker.urltest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UrlTestMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.bunker.urltest.START_MONITOR"
        const val ACTION_STOP = "com.bunker.urltest.STOP_MONITOR"
        private const val CHANNEL_ID = "bunker_url_monitor"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            val intent = Intent(context, UrlTestMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, UrlTestMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var monitorJob: Job? = null
    private val manager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BUNKER Мониторинг сети",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновое отслеживание доступности сервисов и блокировок ТСПУ"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = notification("Запуск мониторинга...", "Проверка связи...")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            val appLabels = installedAppLabels()
            while (isActive) {
                val targets = UrlChecker.targets(this@UrlTestMonitorService)
                val serviceResults = targets.map { UrlChecker.check(it) }

                val selectedAppPackages = getSharedPreferences("selected_apps", Context.MODE_PRIVATE)
                    .getStringSet("packages", emptySet()) ?: emptySet()

                val appResults = selectedAppPackages.map { pkg ->
                    val label = appLabels[pkg] ?: pkg
                    AppNetworkChecker.check(pkg, label)
                }

                val availableServices = serviceResults.count { it.available }
                val totalServices = serviceResults.size
                val availableApps = appResults.count { it.isAvailable }
                val totalApps = appResults.size

                val title = "$availableServices/$totalServices сервисов • $availableApps/$totalApps приложений"
                val body = buildString {
                    val deadServices = serviceResults.filterNot { it.available }
                    if (deadServices.isNotEmpty()) {
                        append("⚠️ Блок: ")
                        append(deadServices.joinToString(", ") { it.title })
                        append("\n")
                    }
                    val deadApps = appResults.filterNot { it.isAvailable }
                    if (deadApps.isNotEmpty()) {
                        append("❌ Недоступно: ")
                        append(deadApps.joinToString(", ") { it.appLabel })
                    } else if (deadServices.isEmpty()) {
                        append("Все проверяемые сервисы доступны")
                    }
                }

                val updatedNotification = notification(title, body)
                manager.notify(NOTIFICATION_ID, updatedNotification)

                UrlTestWidget.render(this@UrlTestMonitorService, serviceResults)

                delay(15_000)
            }
        }

        return START_STICKY
    }

    private fun installedAppLabels(): Map<String, String> {
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = packageManager.queryIntentActivities(launchIntent, 0)
        val map = mutableMapOf<String, String>()
        for (info in activities) {
            val pkg = info.activityInfo.packageName
            val label = try {
                info.loadLabel(packageManager).toString()
            } catch (_: Throwable) {
                pkg
            }
            map[pkg] = label
        }
        return map
    }

    private fun notification(title: String, body: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, UrlTestMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("BUNKER URL TEST • $title")
            .setContentText(body.lines().firstOrNull() ?: body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .addAction(0, "Остановить", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
