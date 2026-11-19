package cat.rellotgeparlant.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import cat.rellotgeparlant.R
import cat.rellotgeparlant.service.Prefs
import cat.rellotgeparlant.service.SpeakerService
import cat.rellotgeparlant.service.WatchdogWorker
import cat.rellotgeparlant.setup.Check
import cat.rellotgeparlant.setup.CheckId
import cat.rellotgeparlant.setup.runChecks
import cat.rellotgeparlant.speech.AndroidTtsEngine
import cat.rellotgeparlant.speech.Speaker

/** Caregiver-only setup screen: test button, speech rate, reliability checklist. */
class MainActivity : AppCompatActivity() {

    private lateinit var voiceProbe: Speaker
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        voiceProbe = Speaker(AndroidTtsEngine(this))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        startService()

        findViewById<Button>(R.id.test_button).setOnClickListener {
            SpeakerService.ensureRunning(this, SpeakerService.ACTION_SPEAK_NOW)
        }
        setUpRateSlider()

        findViewById<SwitchCompat>(R.id.speech_enabled).apply {
            isChecked = Prefs.speechEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> Prefs.setSpeechEnabled(this@MainActivity, checked) }
        }
    }

    override fun onResume() {
        super.onResume()
        startService()
        renderChecks()
        // The service and the voice probe start asynchronously: refresh once they've settled.
        handler.postDelayed({ renderChecks() }, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        voiceProbe.shutdown()
        super.onDestroy()
    }

    private fun startService() {
        SpeakerService.ensureRunning(this)
        WatchdogWorker.schedule(this)
    }

    private fun setUpRateSlider() {
        val label = findViewById<TextView>(R.id.rate_label)
        val bar = findViewById<SeekBar>(R.id.rate)
        fun show(rate: Float) { label.text = getString(R.string.rate_label, rate) }
        val current = Prefs.speechRate(this)
        bar.progress = ((current - MIN_RATE) * 100).toInt()
        show(current)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) {
                show(MIN_RATE + progress / 100f)
            }
            override fun onStartTrackingTouch(s: SeekBar) = Unit
            override fun onStopTrackingTouch(s: SeekBar) {
                Prefs.setSpeechRate(this@MainActivity, MIN_RATE + s.progress / 100f)
                SpeakerService.ensureRunning(this@MainActivity, SpeakerService.ACTION_RATE_CHANGED)
            }
        })
    }

    private fun renderChecks() {
        val container = findViewById<LinearLayout>(R.id.checks)
        container.removeAllViews()
        runChecks(this, voiceProbe.status).forEach { container.addView(row(it)) }
    }

    private fun row(check: Check): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 16)
        }
        val icon = when (check.ok) {
            true -> R.string.status_ok
            false -> R.string.status_bad
            null -> R.string.status_info
        }
        row.addView(TextView(this).apply { text = getString(icon); textSize = 20f; setPadding(0, 0, 16, 0) })
        row.addView(
            TextView(this).apply { text = getString(labelFor(check.id)); textSize = 16f },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        val fix = check.fix
        if (fix != null && check.ok != true) {
            row.addView(Button(this).apply {
                text = getString(if (check.ok == false) R.string.fix else R.string.open)
                setOnClickListener {
                    try { startActivity(fix) } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
                }
            })
        }
        return row
    }

    private fun labelFor(id: CheckId) = when (id) {
        CheckId.NOTIFICATIONS -> R.string.check_notifications
        CheckId.BATTERY_OPTIMISATION -> R.string.check_battery
        CheckId.CATALAN_VOICE -> R.string.check_voice
        CheckId.SERVICE_RUNNING -> R.string.check_service
        CheckId.SWIPE_LOCK -> R.string.check_lock
        CheckId.NOTIFICATION_WAKE -> R.string.check_wake
        CheckId.OEM_AUTOSTART -> R.string.check_autostart
    }

    companion object {
        private const val MIN_RATE = 0.5f
    }
}
