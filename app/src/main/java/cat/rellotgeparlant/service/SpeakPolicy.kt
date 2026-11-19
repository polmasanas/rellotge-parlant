package cat.rellotgeparlant.service

enum class ScreenEvent { ON, OFF }

/** Decides whether a screen event should announce the time. */
object SpeakPolicy {
    private const val MODE_NORMAL = 0 // AudioManager.MODE_NORMAL

    /**
     * ON speaks only on the lock screen (waking an unlocked phone stays silent);
     * OFF (the screen turning off and locking) always speaks.
     * Never when switched off by the caregiver, ringing or in a call.
     */
    fun shouldSpeak(enabled: Boolean, event: ScreenEvent, keyguardLocked: Boolean, audioMode: Int): Boolean {
        if (!enabled || audioMode != MODE_NORMAL) return false
        return when (event) {
            ScreenEvent.ON -> keyguardLocked
            ScreenEvent.OFF -> true
        }
    }
}
