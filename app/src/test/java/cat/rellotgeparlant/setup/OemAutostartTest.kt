package cat.rellotgeparlant.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OemAutostartTest {
    @Test fun xiaomiAnyCase() {
        val expected = AutostartTarget(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        )
        assertEquals(expected, OemAutostart.targetFor("Xiaomi"))
        assertEquals(expected, OemAutostart.targetFor("XIAOMI"))
    }

    @Test fun samsungHasBatteryScreen() {
        assertEquals("com.samsung.android.lool", OemAutostart.targetFor("samsung")?.packageName)
    }

    @Test fun huaweiOppoVivoKnown() {
        assertEquals("com.huawei.systemmanager", OemAutostart.targetFor("HUAWEI")?.packageName)
        assertEquals("com.coloros.safecenter", OemAutostart.targetFor("OPPO")?.packageName)
        assertEquals("com.vivo.permissionmanager", OemAutostart.targetFor("vivo")?.packageName)
    }

    @Test fun unknownBrandIsNull() {
        assertNull(OemAutostart.targetFor("Google"))
    }
}
