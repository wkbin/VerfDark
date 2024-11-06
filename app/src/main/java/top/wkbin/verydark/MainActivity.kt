package top.wkbin.verydark

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku
import top.wkbin.verydark.shizuku.ShizukuUtils
import top.wkbin.verydark.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity(), Shizuku.OnBinderReceivedListener,
    Shizuku.OnBinderDeadListener, ServiceConnection,
    Shizuku.OnRequestPermissionResultListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CODE = 10001
    }

    private var _userService = mutableStateOf<IUserService?>(null)
    private var shizukuServiceState = false

    private val userServiceArgs = Shizuku
        .UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.name))
        .daemon(false)
        .processNameSuffix("adb_shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainPage(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        initShizuku()
    }

    /**
     * 动态申请Shizuku adb shell权限
     */
    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, "当前shizuku版本不支持动态申请权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (ShizukuUtils.checkPermission()) {
            Toast.makeText(this, "已拥有Shizuku权限", Toast.LENGTH_SHORT).show()
            return
        }

        // 动态申请权限
        Shizuku.requestPermission(PERMISSION_CODE)
    }

    private fun initShizuku() {
        // 添加权限申请监听
        Shizuku.addRequestPermissionResultListener(this)
        // Shizuku服务启动时调用该监听
        Shizuku.addBinderReceivedListenerSticky(this)
        // Shizuku服务终止时调用该监听
        Shizuku.addBinderDeadListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除权限申请监听
        Shizuku.removeRequestPermissionResultListener(this)
        Shizuku.removeBinderReceivedListener(this)
        Shizuku.removeBinderDeadListener(this)
        Shizuku.unbindUserService(userServiceArgs, this, true)
    }

    @Composable
    fun MainPage(modifier: Modifier = Modifier) {
        val userService by remember { _userService }
        var isDark by remember { mutableStateOf(false) }
        var currentLight by remember { mutableIntStateOf(0) }
        val isDeviceRooted = remember { RootChecker.isDeviceRooted() }
        val isDeviceShizuku = remember { ShizukuUtils.checkPermission() }
        val isWork by remember {
            derivedStateOf { isDeviceRooted || isDeviceShizuku }
        }

        LaunchedEffect(userService) {
            isDark = if (userService != null) {
                val cmd = "settings get secure reduce_bright_colors_activated"
                (userService?.execArr(cmd.split(" ").toTypedArray())?.toIntOrNull() ?: 0) == 1
            } else {
                SettingsUtils.getReduceBrightColorsActivated()
            }
            currentLight = if (userService != null) {
                val cmd = "settings get secure  reduce_bright_colors_level"
                userService?.execArr(cmd.split(" ").toTypedArray())?.toIntOrNull() ?: 100
            } else {
                SettingsUtils.getReduceBrightColorsLevel()
            }
        }
        Column(modifier.padding(20.dp)) {
            Text(text = "极暗", fontFamily = FontFamily.Serif, fontSize = 30.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "调暗屏幕，看手机时会更舒适",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(25.dp))
            if (isWork) {
                if (isDeviceShizuku) {
                    if (shizukuServiceState) {
                        StatusCard("Shizuku")
                    } else {
                        WarningCard("Shizuku服务未启动")
                    }
                } else {
                    StatusCard("Root")
                }
            } else {
                WarningCard("权限不足")
            }
            Spacer(modifier = Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "将屏幕调成极暗")
                Switch(checked = isDark, enabled = isWork, onCheckedChange = {
                    isDark = it
                    if (userService != null) {
                        val cmd =
                            "settings put secure reduce_bright_colors_activated ${if (isDark) 1 else 0}"
                        userService?.execArr(cmd.split(" ").toTypedArray())
                    } else {
                        SettingsUtils.setReduceBrightColorsActivated(isDark)
                    }
                })
            }
            Spacer(modifier = Modifier.height(25.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(25.dp))
            Text(
                text = "选项",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(15.dp))
            Text(text = "亮度")
            Slider(
                value = currentLight.toFloat(),
                enabled = isDark && isWork,
                valueRange = 0f..100f,
                onValueChange = {
                    currentLight = it.toInt()
                    if (userService != null) {
                        val cmd =
                            "settings put secure reduce_bright_colors_level ${100 - currentLight}"
                        userService?.execArr(cmd.split(" ").toTypedArray())
                    } else {
                        SettingsUtils.setReduceBrightColorsLevel(100 - currentLight)
                    }
                })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "更暗", fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray
                )
                Text(
                    text = "更亮", fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray
                )
            }

            if (isDeviceShizuku){
                Row(
                    modifier = Modifier
                        .padding(top = 50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(onClick = {
                        judgePermission()
                    }) {
                        Text(text = "判断")
                    }
                    Button(onClick = {
                        connectShizuku()
                    }) {
                        Text(text = "连接")
                    }
                    Button(onClick = {
                        requestShizukuPermission()
                    }) {
                        Text(text = "授权")
                    }
                }
            }
        }
    }

    // 判断权限
    private fun judgePermission() {
        if (!shizukuServiceState) {
            Toast.makeText(this, "Shizuku服务异常", Toast.LENGTH_SHORT).show()
            return
        }

        if (ShizukuUtils.checkPermission()) {
            Toast.makeText(this, "已拥有权限", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未拥有权限", Toast.LENGTH_SHORT).show()
        }
    }

    // 请求权限
    private fun requestPermission() {
        if (!shizukuServiceState) {
            Toast.makeText(this, "Shizuku服务异常", Toast.LENGTH_SHORT).show()
            return
        }

        requestShizukuPermission()
    }

    // 连接Shizuku服务
    private fun connectShizuku() {
        if (!shizukuServiceState) {
            Toast.makeText(this, "Shizuku服务异常", Toast.LENGTH_SHORT).show()
            return
        }

        if (!ShizukuUtils.checkPermission()) {
            Toast.makeText(this, "没有Shizuku权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (_userService.value != null) {
            Toast.makeText(this@MainActivity, "已连接Shizuku服务", Toast.LENGTH_SHORT).show()
            return
        }

        Shizuku.bindUserService(userServiceArgs, this)
    }


    override fun onBinderReceived() {
        shizukuServiceState = true
        Toast.makeText(this@MainActivity, "Shizuku服务已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onBinderDead() {
        shizukuServiceState = false
        _userService.value = null
        Toast.makeText(this@MainActivity, "Shizuku服务已终止", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Toast.makeText(this@MainActivity, "Shizuku服务连接成功", Toast.LENGTH_SHORT).show()
        if (binder != null && binder.pingBinder()) {
            _userService.value = IUserService.Stub.asInterface(binder)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Toast.makeText(this@MainActivity, "Shizuku服务连接断开", Toast.LENGTH_SHORT).show()
        _userService.value = null
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PERMISSION_GRANTED) {
            Toast.makeText(this@MainActivity, "Shizuku授权成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "Shizuku授权失败", Toast.LENGTH_SHORT).show()
        }
    }
}


@Composable
private fun StatusCard(workModel: String) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CheckCircle, "工作中")
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "工作模式：$workModel", fontSize = 16.sp)
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Warning, null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}