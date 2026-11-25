package cat.rellotgeparlant.ui

/** Hand angles in degrees, clockwise from 12 o'clock. */
object ClockHands {
    fun hourAngle(hour: Int, minute: Int): Float = (hour % 12) * 30f + minute * 0.5f
    fun minuteAngle(minute: Int): Float = minute * 6f
}
