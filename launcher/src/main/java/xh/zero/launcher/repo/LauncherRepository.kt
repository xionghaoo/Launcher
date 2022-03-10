package xh.zero.launcher.repo

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xh.zero.launcher.AppLoader
import xh.zero.launcher.Desktop

class LauncherRepository(private val context: Context) {

    companion object {
        private const val TAG = "LauncherRepository"
        private var INSTANCE: LauncherRepository? = null

        fun instance(context: Context) : LauncherRepository {
            if (INSTANCE == null) {
                INSTANCE = LauncherRepository(context)
            }
            return INSTANCE!!
        }
    }

    private val cacheDb: LauncherDB by lazy {
        Room.databaseBuilder(context, LauncherDB::class.java, "launcher.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    fun findItem(packageName: String, complete: (item: DesktopItem?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val r = cacheDb.desktopItemDao().findItem(packageName)
            withContext(Dispatchers.Main) {
                complete(r)
            }
        }
    }

    fun findGroupItem(packageName: String, complete: (item: DesktopItem?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val r = cacheDb.desktopItemDao().findItem(packageName)
            if (r != null) {
                recoverDesktopItem(r)
            }
            withContext(Dispatchers.Main) {
                complete(r)
            }
        }
    }

    fun saveItem(item: DesktopItem, complete: (() -> Unit)? = null) {
        Log.d(TAG, "saveItem: ${item.packageName}: (${item.x}, ${item.y}) --- page:${item.page}")
        CoroutineScope(Dispatchers.Default).launch {
            var str = ""
            item.items.forEachIndexed { index, e ->
                str += e.packageName + if (index < item.items.size - 1)DesktopItem.DELIMITER else ""
            }
            item.data = str
            cacheDb.desktopItemDao().insert(item)
            withContext(Dispatchers.Main) {
                complete?.invoke()
            }
        }
    }

    fun deleteItem(item: DesktopItem, deleteSubItems: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            if (deleteSubItems && item.type == DesktopItem.Type.GROUP) {
                item.items.forEach { e ->
                    deleteItem(e, deleteSubItems)
                }
            }
            cacheDb.desktopItemDao().deleteItem(item)
        }
    }

    fun getMaxAppPage(complete: (page: Int) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val page = cacheDb.desktopItemDao().maxPage()
            withContext(Dispatchers.Main) {
                complete(page)
            }
        }
    }

    fun recoverSingleItem(item: DesktopItem, complete: (DesktopItem) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            recoverDesktopItem(item)
            withContext(Dispatchers.Main) {
                complete(item)
            }
        }
    }

    fun requestInstalledApps(context: Context, complete: (Int) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            // 重新加载apps
            cacheDb.desktopItemDao().clear()

            val apps = AppLoader.instance(context).getInstalledApps()
            val tmp = ArrayList<DesktopItem>()
            apps.forEach { app ->
                val item = DesktopItem.createApp(app)
//                if (Configs.babyBusAppMap.containsKey(app.packageName)) {
//                    // 宝宝巴士App
//                    item.isBabyBus = true
//                    item.page = Configs.babyBusAppMap[app.packageName] ?: -1
//                    cacheDb.desktopItemDao().insert(item)
//                } else {
//                    item.isBabyBus = false
//                    tmp.add(item)
//                }
                tmp.add(item)
            }
            val appCount = tmp.size
            val appPageNum = if (appCount % Desktop.PAGE_APP_NUM == 0) appCount / Desktop.PAGE_APP_NUM else appCount / Desktop.PAGE_APP_NUM + 1
            for (i in 0 until appPageNum) {
                val start = i * Desktop.PAGE_APP_NUM
                var end = i * Desktop.PAGE_APP_NUM + Desktop.PAGE_APP_NUM
                if (end >= tmp.size) {
                    end = tmp.size
                }
                // 0 - 24
                // 24 - 42
                val subList = tmp.slice(start until end)
//                Timber.d("load app: start: $start - end: $end, total: ${tmp.size}, sublist: ${subList.size}")
                subList.forEach { e ->
                    e.page = i
//                    Timber.d("insert: ${e.packageName}, ${e.page}")
                    cacheDb.desktopItemDao().insert(e)
                }
            }

            withContext(Dispatchers.Main) {
                complete.invoke(appCount)
            }
        }
    }

    fun queryAppsCount(complete: (Int) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
        val count = cacheDb.desktopItemDao().totalCount()
            complete(count)
        }
    }

    fun findAllItemsForPage(page: Int, complete: (List<DesktopItem>) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val r = cacheDb.desktopItemDao().findAllForPage(page)
            r.forEach { item ->
                recoverDesktopItem(item)
            }
            withContext(Dispatchers.Main) {
                complete(r)
            }
        }
    }

    private suspend fun recoverDesktopItem(item: DesktopItem) {
        item.intent = DesktopItem.getIntentFromApp(item)
        item.type = DesktopItem.Type.valueOf(item.typeStr)
        item.state = DesktopItem.ItemState.values()[item.stateVar]
        val app = AppLoader.instance(context).findApp(item.intent)
        if (app != null) {
            item.icon = app.icon
        }
//        item.icon = Tool.getIcon(context, item.packageName)

        if (item.data?.isNotEmpty() == true) {
            val strList = item.data?.split(DesktopItem.DELIMITER)
            strList?.forEach { pkg ->
                val r = cacheDb.desktopItemDao().findItem(pkg)
                if (r != null) {
                    recoverDesktopItem(r)
                    item.items.add(r)
                }
            }
        }
    }

}