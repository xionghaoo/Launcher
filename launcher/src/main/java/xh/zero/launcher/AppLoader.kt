package xh.zero.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xh.zero.launcher.repo.App
import java.text.Collator
import java.util.*
import kotlin.collections.ArrayList

class AppLoader(
    private val context: Context
) {
    companion object {
        private var INSTANCE: AppLoader? = null
        fun instance(context: Context) : AppLoader {
            if (INSTANCE == null) {
                INSTANCE = AppLoader(context)
            }
            return INSTANCE!!
        }
    }

    private var packageManager = context.packageManager

    var installedApps = Vector<App>()
        private set

//    private val mutex = Mutex()

    fun findApp(intent: Intent?): App? {
        if (intent == null || intent.component == null) return null
        val packageName = intent.component!!.packageName
        val className = intent.component!!.className
        for (app in installedApps) {
            if (app.className == className && app.packageName == packageName) {
                return app
            }
        }
        return null
    }

    fun getInstalledApps(complete: (apps: List<App>) -> Unit) {
        if (installedApps.isNotEmpty()) {
            complete(installedApps)
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                val apps = getInstalledApps()
                withContext(Dispatchers.Main) {
                    complete(apps)
                }
            }
        }
    }

    fun getInstalledApps(): List<App> {
        val result = Vector<App>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val profiles = launcherApps.profiles
            profiles.forEach { userHandle ->
                val apps = launcherApps.getActivityList(null, userHandle)
                apps.forEach { info ->
                    val app = App(packageManager, info)
                    app.userHandle = userHandle
                    if (app.packageName != BuildConfig.packageName
                        // 过滤掉九学王App
                        /*&& !JXWAppManager.packages.containsValue(app.packageName)*/) {
                        Tool.saveIcon(context, Tool.drawableToBitmap(app.icon), app.packageName)
                        result.add(app)
                    }
                }
            }
        } else {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = packageManager.queryIntentActivities(intent, 0)
            activities.forEach { info ->
                val app = App(packageManager, info)
                if (app.packageName != BuildConfig.packageName
                    // 过滤掉九学王App
                    /*&& !JXWAppManager.packages.containsValue(app.packageName)*/) {
                    Tool.saveIcon(context, Tool.drawableToBitmap(app.icon), app.packageName)
                    result.add(app)
                }
            }
        }

        // sort the apps by label here
        result.sortWith(Comparator { one, two ->
            Collator.getInstance().compare(one.label, two.label)
        })
        installedApps = result
        return result
    }
}