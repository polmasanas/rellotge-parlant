package cat.rellotgeparlant.service

import cat.rellotgeparlant.service.ScreenEvent.OFF
import cat.rellotgeparlant.service.ScreenEvent.ON
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakPolicyTest {
    // AudioManager.MODE_* values
    private val normal = 0
    private val ringtone = 1
    private val inCall = 2
    private val inCommunication = 3

    private fun speak(event: ScreenEvent, enabled: Boolean = true, locked: Boolean = true, mode: Int = normal) =
        SpeakPolicy.shouldSpeak(enabled, event, locked, mode)

    @Test fun wakeOnLockScreenSpeaks() = assertTrue(speak(ON))
    @Test fun wakeWhenUnlockedIsSilent() = assertFalse(speak(ON, locked = false))
    @Test fun screenOffSpeaks() = assertTrue(speak(OFF, locked = false))
    @Test fun disabledSilentOnWake() = assertFalse(speak(ON, enabled = false))
    @Test fun disabledSilentOnScreenOff() = assertFalse(speak(OFF, enabled = false))
    @Test fun silentWhileRinging() = assertFalse(speak(ON, mode = ringtone))
    @Test fun silentInCall() = assertFalse(speak(ON, mode = inCall))
    @Test fun screenOffDuringCallIsSilent() = assertFalse(speak(OFF, mode = inCall))
    @Test fun silentInVoip() = assertFalse(speak(OFF, mode = inCommunication))
}
