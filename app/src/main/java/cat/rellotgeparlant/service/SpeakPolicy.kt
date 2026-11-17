package cat.rellotgeparlant.service

/** Decides whether a screen wake should announce the time. */
object SpeakPolicy {
    private const val MODE_NORMAL = 0 // AudioManager.MODE_NORMAL

    fun shouldSpeak(keyguardLocked: Boolean, audioMode: Int): Boolean =
        keyguardLocked && audioMode == MODE_NORMAL
}
