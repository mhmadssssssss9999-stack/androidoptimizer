package com.redmi14c.optimizer

import android.content.Context
import android.os.Build
import com.redmi14c.optimizer.shizuku.ShizukuManager

object DeviceDiagnostics {

    data class Report(
        val model: String,
        val manufacturer: String,
        val androidVersion: String,
        val sdk: Int,
        val chipset: String,
        val abi: String,
        val shizukuUid: Int,
        val shizukuMode: String,
        val cpuInfo: String,
        val gpuInfo: String,
        val refreshRate: String,
        val thermalInfo: String,
        val storageInfo: String,
        val powerInfo: String
    )

    suspend fun collect(context: Context): Report {
        val uid = ShizukuManager.getUid()

        val mode = when (uid) {
            0 -> "Root"
            2000 -> "ADB / Shizuku"
            -1 -> "Unavailable"
            else -> "UID $uid"
        }

        val cpuInfo = runCommand(
            "getprop ro.hardware; getprop ro.board.platform; getprop ro.product.cpu.abi"
        )

        val gpuInfo = runCommand(
            "getprop ro.hardware.egl; getprop ro.opengles.version"
        )

        val refreshRate = runCommand(
            "settings get system peak_refresh_rate; " +
                "settings get system min_refresh_rate; " +
                "settings get system user_refresh_rate"
        )

        val thermalInfo = runCommand(
            "for z in /sys/class/thermal/thermal_zone*/type; do " +
                "echo \"TYPE: \$z\"; cat \"\$z\" 2>/dev/null; " +
                "done"
        )

        val storageInfo = runCommand(
            "cat /proc/partitions 2>/dev/null | tail -n +3"
        )

        val powerInfo = runCommand(
            "cmd power get-fixed-performance-mode-enabled 2>/dev/null; " +
                "settings get global low_power"
        )

        return Report(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            chipset = Build.HARDWARE,
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            shizukuUid = uid,
            shizuku
