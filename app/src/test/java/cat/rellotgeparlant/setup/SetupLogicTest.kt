package cat.rellotgeparlant.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupLogicTest {
    @Test fun halfOrMoreIsOk() = assertTrue(alarmVolumeOk(4, 7))
    @Test fun belowHalfIsLow() = assertFalse(alarmVolumeOk(3, 7))
    @Test fun noRangeIsLow() = assertFalse(alarmVolumeOk(0, 0))
}
