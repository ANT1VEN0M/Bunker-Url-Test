package com.bunker.urltest

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class UrlTestWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            show(context, appWidgetManager, id)
        }
        val request = OneTimeWorkRequest.Builder(UrlTestWorker::class.java).build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        private fun views(context: Context): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_url_test)
            val openIntent = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val refreshIntent = PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, UrlTestWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.widget_root, openIntent)
            v.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent)
            return v
        }

        fun show(context: Context, manager: AppWidgetManager, id: Int) {
            manager.updateAppWidget(id, views(context))
        }

        fun render(context: Context, data: List<CheckResult>) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, UrlTestWidget::class.java)) ?: return
            val mapping = mapOf(
                "google" to R.id.widget_google,
                "telegram" to R.id.widget_telegram,
                "instagram" to R.id.widget_instagram,
                "youtube" to R.id.widget_youtube,
                "custom" to R.id.widget_custom
            )

            for (widgetId in ids) {
                val v = views(context)
                for (result in data) {
                    val targetViewId = mapping[result.id] ?: if (result.id.startsWith("custom_")) R.id.widget_custom else null
                    if (targetViewId != null) {
                        v.setTextViewText(targetViewId, "${result.title}  •  ${result.label}")
                        val textColor = if (result.available) Color.parseColor("#45b879") else Color.parseColor("#e0444e")
                        v.setTextColor(targetViewId, textColor)
                        if (targetViewId == R.id.widget_custom) {
                            v.setViewVisibility(targetViewId, View.VISIBLE)
                        }
                    }
                }
                manager.updateAppWidget(widgetId, v)
            }
        }
    }
}
