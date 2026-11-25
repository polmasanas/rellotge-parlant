package cat.rellotgeparlant.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockHandsTest {
    private val d = 0.001f

    @Test fun twelveOClockPointsUp() {
        assertEquals(0f, ClockHands.hourAngle(0, 0), d)
        assertEquals(0f, ClockHands.hourAngle(12, 0), d)
        assertEquals(0f, ClockHands.minuteAngle(0), d)
    }

    @Test fun threeOClock() = assertEquals(90f, ClockHands.hourAngle(15, 0), d)

    @Test fun hourHandMovesWithMinutes() = assertEquals(135f, ClockHands.hourAngle(4, 30), d)

    @Test fun minuteHand() {
        assertEquals(90f, ClockHands.minuteAngle(15), d)
        assertEquals(354f, ClockHands.minuteAngle(59), d)
    }
}
