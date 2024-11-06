package top.wkbin.verydark

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootChecker {

    // 检查常见的root二进制文件路径
    private val ROOT_PATHS = arrayOf(
        "/system/bin/",
        "/system/xbin/",
        "/sbin/",
        "/system/sd/xbin/",
        "/system/bin/failsafe/",
        "/data/local/xbin/",
        "/data/local/bin/",
        "/data/local/"
    )

    // 检查root权限
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    // 方法1：检查常见的root二进制文件
    private fun checkRootMethod1(): Boolean {
        for (path in ROOT_PATHS) {
            if (File(path + "su").exists()) {
                return true
            }
        }
        return false
    }

    // 方法2：检查能够执行su命令
    private fun checkRootMethod2(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (t: Throwable) {
            false
        }
    }

    // 方法3：尝试执行su命令并检查返回码
    private fun checkRootMethod3(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        }
    }
}