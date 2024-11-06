package top.wkbin.verydark.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuUtils {

    fun checkPermission() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

}