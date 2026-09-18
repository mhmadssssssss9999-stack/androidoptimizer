package com.redmi14c.optimizer.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    private const val REQUEST_CODE = 1001

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            isShizukuRunning() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(REQUEST_CODE)
    }

    fun removePermissionListener(
        listener: Shizuku.OnRequestPermissionResultListener
    ) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (e: Exception) {
            -1
        }
    }

    fun isRoot(): Boolean = getUid() == 0

    fun isAdb(): Boolean = getUid() == 2000

    suspend fun executeCommand(command: String): ShellResult =
        withContext(Dispatchers.IO) {
            var process: ShizukuRemoteProcess? = null

            try {
                if (!isShizukuRunning()) {
                    return@withContext ShellResult(
                        success = false,
                        output = "",
                        error = "Shizuku is not running",
                        exitCode = -1
                    )
                }

                if (!hasPermission()) {
                    return@withContext ShellResult(
                        success = false,
                        output = "",
                        error = "Shizuku permission not granted",
                        exitCode = -1
                    )
                }

                process = Shizuku.newProcess(
                    arrayOf("sh", "-c", command),
                    null,
                    null
                )

                val output = StringBuilder()
                val error = StringBuilder()

                val outputReader = BufferedReader(
                    InputStreamReader(process.inputStream)
                )

                val errorReader = BufferedReader(
                    InputStreamReader(process.errorStream)
                )

                outputReader.useLines { lines ->
                    lines.forEach { line ->
                        output.append(line).append('\n')
                    }
                }

                errorReader.useLines { lines ->
                    lines.forEach { line ->
                        error.append(line).append('\n')
                    }
                }

                val exitCode = process.waitFor()

                ShellResult(
                    success = exitCode == 0,
                    output = output.toString().trim(),
                    error = error.toString().trim(),
                    exitCode = exitCode
                )

            } catch (e: Exception) {
                Timber.e(e, "Shizuku command failed: $command")

                ShellResult(
                    success = false,
                    output = "",
                    error = e.message ?: "Unknown Shizuku error",
                    exitCode = -1
                )
            } finally {
                try {
                    process?.destroy()
                } catch (_: Exception) {
                }
            }
        }

    suspend fun executeCommands(
        commands: List<String>
    ): List<ShellResult> {
        return commands.map { executeCommand(it) }
    }

    suspend fun executeCommandWithOutput(
        command: String
    ): String {
        val result = executeCommand(command)

        return if (result.success) {
            result.output
        } else {
            result.error
        }
    }

    fun getShizukuVersion(): String {
        return try {
            if (!isShizukuRunning()) {
                "Not running"
            } else {
                "v${Shizuku.getVersion()}"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

data class ShellResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val exitCode: Int = 0
)
