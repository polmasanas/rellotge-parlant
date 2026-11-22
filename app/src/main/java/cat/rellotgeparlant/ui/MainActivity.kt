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
import android.app.TimePickerDialog
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import cat.rellotgeparlant.R
import cat.rellotgeparlant.calendar.CalendarSource
import cat.rellotgeparlant.calendar.EventSettings
import cat.rellotgeparlant.calendar.ReminderScheduler
import java.time.LocalTime
import kotlin.math.roundToInt
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
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_CALENDAR), 0)
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 0)
        }
        startService()

        findViewById<Button>(R.id.test_button).setOnClickListener {
            SpeakerService.ensureRunning(this, SpeakerService.ACTION_SPEAK_NOW)
        }
        setUpRateSlider()
        setUpVolumeSlider()
        setUpCalendar()

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fillCalendarPicker()
        renderChecks()
    }

    private fun setUpVolumeSlider() {
        val label = findViewById<TextView>(R.id.volume_label)
        val bar = findViewById<SeekBar>(R.id.volume)
        fun show(percent: Int) { label.text = getString(R.string.volume_label, percent) }
        val current = (Prefs.voiceVolume(this) * 100).roundToInt()
        bar.progress = current - MIN_VOLUME_PERCENT
        show(current)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) = show(MIN_VOLUME_PERCENT + progress)
            override fun onStartTrackingTouch(s: SeekBar) = Unit
            override fun onStopTrackingTouch(s: SeekBar) {
                Prefs.setVoiceVolume(this@MainActivity, (MIN_VOLUME_PERCENT + s.progress) / 100f)
                SpeakerService.ensureRunning(this@MainActivity, SpeakerService.ACTION_VOLUME_CHANGED)
            }
        })
    }

    private var calendarIds: List<Long> = emptyList()

    private fun fillCalendarPicker() {
        val picker = findViewById<Spinner>(R.id.calendar_picker)
        val calendars = CalendarSource(this).calendars()
        calendarIds = listOf(-1L) + calendars.map { it.first }
        val names = listOf(getString(R.string.calendar_none)) + calendars.map { it.second }
        picker.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        picker.setSelection(calendarIds.indexOf(Prefs.calendarId(this)).coerceAtLeast(0))
    }

    private fun saveEventSettings(change: (EventSettings) -> EventSettings) {
        Prefs.setEventSettings(this, change(Prefs.eventSettings(this)))
        ReminderScheduler.reschedule(this)
    }

    private fun setUpCalendar() {
        fillCalendarPicker()
        findViewById<Spinner>(R.id.calendar_picker).onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosen = calendarIds.getOrElse(position) { -1L }
                if (chosen != Prefs.calendarId(this@MainActivity)) {
                    Prefs.setCalendarId(this@MainActivity, chosen)
                    ReminderScheduler.reschedule(this@MainActivity)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val settings = Prefs.eventSettings(this)

        val hoursLabel = findViewById<TextView>(R.id.hours_label)
        val hours = findViewById<SeekBar>(R.id.hours)
        hours.progress = settings.hoursAhead - 1
        hoursLabel.text = getString(R.string.hours_label, settings.hoursAhead)
        hours.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) {
                hoursLabel.text = getString(R.string.hours_label, progress + 1)
            }
            override fun onStartTrackingTouch(s: SeekBar) = Unit
            override fun onStopTrackingTouch(s: SeekBar) = saveEventSettings { it.copy(hoursAhead = s.progress + 1) }
        })

        val reminder = findViewById<Spinner>(R.id.reminder)
        reminder.setSelection(REMINDER_MINUTES.indexOf(settings.reminderMinutes).coerceAtLeast(0))
        reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val minutes = REMINDER_MINUTES[position]
                if (minutes != Prefs.eventSettings(this@MainActivity).reminderMinutes) {
                    saveEventSettings { it.copy(reminderMinutes = minutes) }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val morning = findViewById<Button>(R.id.morning_time)
        fun showMorning(t: LocalTime) { morning.text = getString(R.string.morning_label, "%02d:%02d".format(t.hour, t.minute)) }
        showMorning(settings.morningTime)
        morning.setOnClickListener {
            val t = Prefs.eventSettings(this).morningTime
            TimePickerDialog(this, { _, h, m ->
                val chosen = LocalTime.of(h, m)
                saveEventSettings { it.copy(morningTime = chosen) }
                showMorning(chosen)
            }, t.hour, t.minute, true).show()
        }

        findViewById<SwitchCompat>(R.id.read_location).apply {
            isChecked = settings.readLocation
            setOnCheckedChangeListener { _, on -> saveEventSettings { it.copy(readLocation = on) } }
        }
        findViewById<SwitchCompat>(R.id.read_description).apply {
            isChecked = settings.readDescription
            setOnCheckedChangeListener { _, on -> saveEventSettings { it.copy(readDescription = on) } }
        }
        findViewById<Button>(R.id.test_calendar).setOnClickListener {
            SpeakerService.ensureRunning(this, SpeakerService.ACTION_SPEAK_EVENTS_TEST)
        }
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
        CheckId.ALARM_VOLUME -> R.string.check_alarm_volume
        CheckId.CALENDAR_PERMISSION -> R.string.check_calendar
        CheckId.SWIPE_LOCK -> R.string.check_lock
        CheckId.NOTIFICATION_WAKE -> R.string.check_wake
        CheckId.OEM_AUTOSTART -> R.string.check_autostart
    }

    companion object {
        private const val MIN_RATE = 0.5f
        private const val MIN_VOLUME_PERCENT = 10
        private val REMINDER_MINUTES = listOf(0, 15, 30, 60, 120)
    }
}
