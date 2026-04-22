package com.example.reolinknotif
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

const val REOLINK_PKG = "com.mcu.reolink"
const val ACTION_TIMER_EXPIRED = "com.example.reolinknotif.TIMER_EXPIRED"
private const val PREFS = "reolink_prefs"
private const val KEY_END = "snooze_end_ms"

object TimerManager {
    fun start(ctx: Context, ms: Long) {
        val end = System.currentTimeMillis() + ms
        prefs(ctx).edit().putLong(KEY_END, end).apply()
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi(ctx))
    }
    fun cancel(ctx: Context) {
        prefs(ctx).edit().remove(KEY_END).apply()
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(ctx))
    }
    fun isActive(ctx: Context) = prefs(ctx).getLong(KEY_END, 0L) > System.currentTimeMillis()
    fun remainingMs(ctx: Context) = maxOf(0L, prefs(ctx).getLong(KEY_END, 0L) - System.currentTimeMillis())
    private fun pi(ctx: Context) = PendingIntent.getBroadcast(ctx, 99,
        Intent(ctx, ReolinkWidget::class.java).also { it.action = ACTION_TIMER_EXPIRED },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}