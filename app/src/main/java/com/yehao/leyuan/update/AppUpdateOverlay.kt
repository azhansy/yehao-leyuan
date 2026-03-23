package com.yehao.leyuan.update

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yehao.leyuan.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private sealed class UpdateUi {
    data object Hidden : UpdateUi()
    data class Prompt(val info: RemoteUpdateInfo) : UpdateUi()
    data class Downloading(val progress: Float) : UpdateUi()
    data class NeedPermission(val file: File) : UpdateUi()
    data class Failed(val message: String) : UpdateUi()
}

@Composable
fun AppUpdateOverlay() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()
    val prefs = remember { AppUpdatePrefs(context) }
    var phase by remember { mutableStateOf<UpdateUi>(UpdateUi.Hidden) }
    var permissionApk by remember { mutableStateOf<File?>(null) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val apk = permissionApk
        permissionApk = null
        if (apk != null && activity.canInstallPackagesCompat()) {
            activity.launchApkInstall(apk)
        }
        phase = UpdateUi.Hidden
    }

    LaunchedEffect(Unit) {
        delay(400)
        val base = BuildConfig.UPDATE_BASE_URL.trim()
        if (base.isEmpty()) return@LaunchedEffect
        val remote = fetchRemoteUpdateInfo(base) ?: return@LaunchedEffect
        if (remote.versionCode <= BuildConfig.VERSION_CODE) return@LaunchedEffect
        if (prefs.ignoredVersionCode == remote.versionCode) return@LaunchedEffect
        phase = UpdateUi.Prompt(remote)
    }

    when (val p = phase) {
        UpdateUi.Hidden -> Unit
        is UpdateUi.Prompt -> {
            var wifiOnly by remember(p.info.versionCode) {
                mutableStateOf(prefs.wifiOnlyDownload)
            }
            AlertDialog(
                onDismissRequest = { },
                title = { Text("发现新版本") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val vn = p.info.versionName
                        Text(
                            if (vn != null) {
                                "版本 $vn（${p.info.versionCode}）\n是否下载并安装？"
                            } else {
                                "版本号 ${p.info.versionCode}\n是否下载并安装？"
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = wifiOnly,
                                onCheckedChange = {
                                    wifiOnly = it
                                    prefs.wifiOnlyDownload = it
                                },
                            )
                            Text("仅 Wi‑Fi 下载")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (wifiOnly && !context.isOnWifi()) {
                                Toast.makeText(
                                    context,
                                    "已开启仅 Wi‑Fi 下载，请连接 Wi‑Fi 后再试",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@TextButton
                            }
                            val info = p.info
                            scope.launch {
                                phase = UpdateUi.Downloading(0f)
                                val apkFile = File(context.cacheDir, "apk_updates/latest.apk")
                                val ok = downloadApkToFile(
                                    BuildConfig.UPDATE_BASE_URL,
                                    info.apkFileName,
                                    apkFile,
                                ) { pr ->
                                    phase = UpdateUi.Downloading(pr)
                                }
                                if (!ok) {
                                    phase = UpdateUi.Failed("下载失败，请稍后重试")
                                    return@launch
                                }
                                if (!activity.canInstallPackagesCompat()) {
                                    phase = UpdateUi.NeedPermission(apkFile)
                                    return@launch
                                }
                                activity.launchApkInstall(apkFile)
                                phase = UpdateUi.Hidden
                            }
                        },
                    ) { Text("立即更新") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            prefs.ignoredVersionCode = p.info.versionCode
                            phase = UpdateUi.Hidden
                        },
                    ) { Text("本次忽略") }
                },
            )
        }
        is UpdateUi.Downloading -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("正在下载") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { p.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(p.progress * 100).toInt()}%")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {}, enabled = false) { Text("请稍候") }
                },
            )
        }
        is UpdateUi.NeedPermission -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("需要安装权限") },
                text = { Text("请在设置中允许本应用「安装未知应用」，然后返回再次尝试更新。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            permissionApk = p.file
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            ).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            settingsLauncher.launch(intent)
                        },
                    ) { Text("去设置") }
                },
                dismissButton = {
                    TextButton(onClick = { phase = UpdateUi.Hidden }) { Text("取消") }
                },
            )
        }
        is UpdateUi.Failed -> {
            AlertDialog(
                onDismissRequest = { phase = UpdateUi.Hidden },
                title = { Text("更新失败") },
                text = { Text(p.message) },
                confirmButton = {
                    TextButton(onClick = { phase = UpdateUi.Hidden }) { Text("确定") }
                },
            )
        }
    }
}
