package cat.rellotgeparlant.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cat.rellotgeparlant.R
import cat.rellotgeparlant.service.SpeakerService
import cat.rellotgeparlant.service.WatchdogWorker

/** Her screen: one big button that says the time. The small cogwheel opens the settings. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        findViewById<android.view.View>(R.id.say_time).setOnClickListener {
            SpeakerService.ensureRunning(this, SpeakerService.ACTION_SPEAK_NOW)
        }
        findViewById<android.view.View>(R.id.open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        SpeakerService.ensureRunning(this)
        WatchdogWorker.schedule(this)
    }
}
