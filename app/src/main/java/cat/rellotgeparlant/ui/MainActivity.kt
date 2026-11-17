package cat.rellotgeparlant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cat.rellotgeparlant.service.SpeakerService
import cat.rellotgeparlant.service.WatchdogWorker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeakerService.ensureRunning(this)
        WatchdogWorker.schedule(this)
    }
}
