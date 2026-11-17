package cat.rellotgeparlant.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakPolicyTest {
    // AudioManager.MODE_* values
    private val normal = 0
    private val ringtone = 1
    private val inCall = 2
    private val inCommunication = 3

    @Test fun speaksWhenLockedAndIdle() = assertTrue(SpeakPolicy.shouldSpeak(true, normal))
    @Test fun silentWhenUnlocked() = assertFalse(SpeakPolicy.shouldSpeak(false, normal))
    @Test fun silentWhileRinging() = assertFalse(SpeakPolicy.shouldSpeak(true, ringtone))
    @Test fun silentInCall() = assertFalse(SpeakPolicy.shouldSpeak(true, inCall))
    @Test fun silentInVoip() = assertFalse(SpeakPolicy.shouldSpeak(true, inCommunication))
}
