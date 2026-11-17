package cat.rellotgeparlant.setup

data class AutostartTarget(val packageName: String, val className: String)

/** Known "allow autostart / don't kill in background" screens of aggressive phone brands. */
object OemAutostart {
    private val TARGETS = mapOf(
        "xiaomi" to AutostartTarget("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        "huawei" to AutostartTarget("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        "oppo" to AutostartTarget("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        "vivo" to AutostartTarget("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        "samsung" to AutostartTarget("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
    )

    fun targetFor(manufacturer: String): AutostartTarget? = TARGETS[manufacturer.lowercase()]
}
