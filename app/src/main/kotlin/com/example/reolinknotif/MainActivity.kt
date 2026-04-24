package com.example.reolinknotif

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var btnToggle: ImageButton? = null
    private var tvStatus: TextView? = null
    private var tvHint: TextView? = null
    private var countdownCard: LinearLayout? = null
    private var tvCountdown: TextView? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        try {
            setContentView(R.layout.activity_main)
            btnToggle     = findViewById(R.id.btnToggle)
            tvStatus      = findViewById(R.id.tvStatus)
            tvHint        = findViewById(R.id.tvHint)
            countdownCard = findViewById(R.id.countdownCard)
            tvCountdown   = findViewById(R.id.tvCountdown)

            btnToggle?.setOnClickListener { openSettings() }

            findViewById<Button>(R.id.btnCancelTimer)?.setOnClickListener {
                TimerManager.cancel(this); updateUI(); refresh()
            }
            findViewById<Button>(R.id.chip30m)?.setOnClickListener  { snooze(TimeUnit.MINUTES.toMillis(30)) }
            findViewById<Button>(R.id.chip1h)?.setOnClickListener   { snooze(TimeUnit.HOURS.toMillis(1)) }
            findViewById<Button>(R.id.chip4h)?.setOnClickListener   { snooze(TimeUnit.HOURS.toMillis(4)) }
            findViewById<Button>(R.id.chip10h)?.setOnClickListener  { snooze(TimeUnit.HOURS.toMillis(10)) }
            findViewById<Button>(R.id.chip24h)?.setOnClickListener  { snooze(TimeUnit.HOURS.toMillis(24)) }

            Toast.makeText(this, "Reolink Notifiche avviata!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Errore avvio: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateUI()
            if (TimerManager.isActive(this)) tick()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore UI: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    private fun snooze(ms: Long) {
        try {
            TimerManager.start(this, ms)
            openSettings()
            updateUI()
            refresh()
            tick()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore timer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            val r = TimerManager.remainingMs(this@MainActivity)
            if (r <= 0) { TimerManager.cancel(this@MainActivity); updateUI(); refresh() }
            else { showCd(r); handler.postDelayed(this, 1000) }
        }
    }

    private fun tick() { handler.removeCallbacks(ticker); handler.post(ticker) }

    private fun showCd(ms: Long) {
        val h = TimeUnit.MILLISECONDS.toHours(ms)
        val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        tvCountdown?.text = "%02d:%02d:%02d".format(h, m, s)
    }

    private fun notifsEnabled(): Boolean {
        return try {
            val pkgCtx = createPackageContext(REOLINK_PKG, 0)
            NotificationManagerCompat.from(pkgCtx).areNotificationsEnabled()
        } catch (e: Exception) { true }
    }

    private fun updateUI() {
        val snoozed = TimerManager.isActive(this)
        val on = notifsEnabled()
        when {
            snoozed -> {
                btnToggle?.setImageResource(R.drawable.ic_bell_off)
                btnToggle?.setBackgroundResource(R.drawable.bg_snooze)
                tvStatus?.text = "SILENZIATE"
                tvStatus?.setTextColor(getColor(R.color.orange))
                tvHint?.text = "Timer attivo — riattivazione automatica"
                countdownCard?.visibility = View.VISIBLE
                showCd(TimerManager.remainingMs(this))
            }
            on -> {
                btnToggle?.setImageResource(R.drawable.ic_bell_on)
                btnToggle?.setBackgroundResource(R.drawable.bg_on)
                tvStatus?.text = "NOTIFICHE ATTIVE"
                tvStatus?.setTextColor(getColor(R.color.green))
                tvHint?.text = "Tocca per aprire le impostazioni notifiche"
                countdownCard?.visibility = View.GONE
            }
            else -> {
                btnToggle?.setImageResource(R.drawable.ic_bell_off)
                btnToggle?.setBackgroundResource(R.drawable.bg_off)
                tvStatus?.text = "NOTIFICHE DISATTIVATE"
                tvStatus?.setTextColor(getColor(R.color.grey))
                tvHint?.text = "Tocca per riattivarle dalle impostazioni"
                countdownCard?.visibility = View.GONE
            }
        }
    }

    private fun openSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, REOLINK_PKG)
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Impossibile aprire impostazioni: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun refresh() {
        try {
            val mgr = AppWidgetManager.getInstance(this)
            mgr.getAppWidgetIds(ComponentName(this, ReolinkWidget::class.java))
                .forEach { ReolinkWidget.update(this, mgr, it) }
        } catch (e: Exception) { /* ignora */ }
    }
}
