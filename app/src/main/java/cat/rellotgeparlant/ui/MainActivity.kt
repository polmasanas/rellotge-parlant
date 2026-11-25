package cat.rellotgeparlant.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cat.rellotgeparlant.R
import cat.rellotgeparlant.service.SpeakerService
import cat.rellotgeparlant.service.WatchdogWorker

/** Her screen: one big round clock that says the time. The cogwheel opens settings after a 2-second hold. */
class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var settingsOpened = false
    private val openSettings = Runnable {
        settingsOpened = true
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        findViewById<View>(R.id.say_time).setOnClickListener {
            SpeakerService.ensureRunning(this, SpeakerService.ACTION_SPEAK_NOW)
        }

        val cog = findViewById<View>(R.id.open_settings)
        cog.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    settingsOpened = false
                    v.isPressed = true
                    handler.postDelayed(openSettings, HOLD_MS)
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    handler.removeCallbacks(openSettings)
                    if (!settingsOpened) Toast.makeText(this, R.string.hold_for_settings, Toast.LENGTH_SHORT).show()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(openSettings)
                }
            }
            true
        }
        // Screen readers (TalkBack) can't hold: let them open it with their own action.
        cog.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        SpeakerService.ensureRunning(this)
        WatchdogWorker.schedule(this)
    }

    override fun onPause() {
        handler.removeCallbacks(openSettings)
        super.onPause()
    }

    companion object {
        private const val HOLD_MS = 2_000L
    }
}
