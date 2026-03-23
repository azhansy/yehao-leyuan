package com.yehao.leyuan.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

fun Activity.canInstallPackagesCompat(): Boolean =
    packageManager.canRequestPackageInstalls()

fun Activity.openInstallPermissionSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:$packageName")
        }
    } else {
        Intent(Settings.ACTION_SECURITY_SETTINGS)
    }
    startActivity(intent)
}

fun Activity.launchApkInstall(apk: File) {
    val uri = FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        apk,
    )
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        data = uri
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    startActivity(intent)
}
