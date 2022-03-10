package xh.zero.launcher.repo

import android.os.UserHandle
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.annotation.SuppressLint
import android.content.pm.LauncherActivityInfo
import xh.zero.launcher.repo.App
import android.content.ComponentName
import android.graphics.drawable.Drawable

class App {
    var icon: Drawable
    var label: String
    var packageName: String
    var className: String
    var userHandle: UserHandle? = null
    var isLimited = false

    constructor(pm: PackageManager?, info: ResolveInfo) {
        icon = info.loadIcon(pm)
        label = info.loadLabel(pm).toString()
        packageName = info.activityInfo.packageName
        className = info.activityInfo.name
    }

    constructor(pm: PackageManager?, info: LauncherActivityInfo) {
        icon = info.getIcon(0)
        label = info.label.toString()
        packageName = info.componentName.packageName
        className = info.name
    }

    override fun equals(obj: Any?): Boolean {
        return if (obj is App) {
            packageName == obj.packageName
        } else {
            false
        }
    }

    val componentName: String
        get() = ComponentName(packageName, className).toString()
}