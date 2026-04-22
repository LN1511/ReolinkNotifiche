package com.example.reolinknotif
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat

const val ACTION_TAP = "com.example.reolinknotif.WIDGET_TAP"

class ReolinkWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) =
        ids.forEach { update(ctx, mgr, it) }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            ACTION_TAP -> {
                if (TimerManager.isActive(ctx))
                    ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                else
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, REOLINK_PKG)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                refreshAll(ctx)
            }
            ACTION_TIMER_EXPIRED -> { TimerManager.cancel(ctx); refreshAll(ctx) }
        }
    }

    private fun refreshAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        mgr.getAppWidgetIds(ComponentName(ctx, ReolinkWidget::class.java)).forEach { update(ctx, mgr, it) }
    }

    companion object {
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val snoozed = TimerManager.isActive(ctx)
            val on = try { NotificationManagerCompat.from(ctx.createPackageContext(REOLINK_PKG, 0)).areNotificationsEnabled() } catch (e: Exception) { true }
            val v = RemoteViews(ctx.packageName, R.layout.widget_layout)
            when {
                snoozed -> {
                    v.setImageViewResource(R.id.ivBell, R.drawable.ic_bell_off)
                    v.setTextViewText(R.id.tvState, "⏱")
                    v.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.bg_snooze)
                    val rem = TimerManager.remainingMs(ctx)
                    val h = rem / 3_600_000; val m = (rem % 3_600_000) / 60_000
                    v.setTextViewText(R.id.tvCountdownWidget, if (h > 0) "%dh %02dm".format(h, m) else "%dm".format(m))
                    v.setViewVisibility(R.id.tvCountdownWidget, View.VISIBLE)
                }
                on -> {
                    v.setImageViewResource(R.id.ivBell, R.drawable.ic_bell_on)
                    v.setTextViewText(R.id.tvState, "ON")
                    v.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.bg_on)
                    v.setViewVisibility(R.id.tvCountdownWidget, View.GONE)
                }
                else -> {
                    v.setImageViewResource(R.id.ivBell, R.drawable.ic_bell_off)
                    v.setTextViewText(R.id.tvState, "OFF")
                    v.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.bg_off)
                    v.setViewVisibility(R.id.tvCountdownWidget, View.GONE)
                }
            }
            val pi = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, ReolinkWidget::class.java).also { it.action = ACTION_TAP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.widgetRoot, pi)
            mgr.updateAppWidget(id, v)
        }
    }
}