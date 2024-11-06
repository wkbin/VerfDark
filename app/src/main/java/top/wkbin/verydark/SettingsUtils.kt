package top.wkbin.verydark


import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


object SettingsUtils {

    private const val TAG = "SettingsUtils"

    // 极暗模式开关
    private const val KEY_REDUCE_BRIGHT_COLORS_ACTIVATED = "reduce_bright_colors_activated"

    // 极暗模式变暗百分比
    private const val KEY_REDUCE_BRIGHT_COLORS_LEVEL = "reduce_bright_colors_level"

    /**
     * 执行命令并返回输出
     */
    private fun executeCommand(command: String): String {
        val output = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            reader.close()
            process.waitFor()
        } catch (e: IOException) {
            Log.i(TAG, "$command error: ${e.message}")
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return output.toString().trim()
    }

    fun getReduceBrightColorsActivated(): Boolean {
        val cmd = "settings get secure $KEY_REDUCE_BRIGHT_COLORS_ACTIVATED"
        return (executeCommand(cmd).toIntOrNull() ?: 0) == 1
    }

    fun setReduceBrightColorsActivated(activated: Boolean) {
        val cmd =
            "settings put secure $KEY_REDUCE_BRIGHT_COLORS_ACTIVATED ${if (activated) 1 else 0}"
        executeCommand(cmd)
    }

    fun getReduceBrightColorsLevel(): Int {
        val cmd = "settings get secure $KEY_REDUCE_BRIGHT_COLORS_LEVEL"
        return executeCommand(cmd).toIntOrNull() ?: 100
    }

    fun setReduceBrightColorsLevel(level: Int) {
        val cmd = "settings put secure $KEY_REDUCE_BRIGHT_COLORS_LEVEL $level"
        executeCommand(cmd)
    }
}