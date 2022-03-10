package xh.zero.launcher.repo

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import xh.zero.launcher.LauncherConfig
import java.util.*
import kotlin.collections.ArrayList

@Entity(tableName = "DesktopItem")
class DesktopItem {
    @PrimaryKey
    var packageName: String = ""
    var className: String? = null
    var label: String? = null

    var x: Int = -1
    var y: Int = -1
    var data: String? = null
    var stateVar: Int = 1
    var typeStr: String = ""

    var folderPackageName: String? = null

    var page: Int = -1

    @Ignore
    var items: ArrayList<DesktopItem> = ArrayList()
    @Ignore
    var icon: Drawable? = null
    @Ignore
    var intent: Intent? = null
    @Ignore
    var type: Type? = null
    @Ignore
    var state: ItemState? = null
    @Ignore
    var startArgs: Map<String, String>? = null

    enum class Type {
        APP, GROUP
    }

    enum class ItemState {
        Hidden, Visible
    }

    companion object {
        const val DELIMITER = "#"

        fun createApp(app: App) : DesktopItem {
            val thirdApp = DesktopItem()
            thirdApp.label = app.label
            thirdApp.packageName = app.packageName
            thirdApp.className = app.className
            thirdApp.icon = app.icon
            thirdApp.stateVar = 1
            thirdApp.state = ItemState.Visible
            thirdApp.typeStr = Type.APP.toString()
            thirdApp.type = Type.APP
            thirdApp.intent = getIntentFromApp(app)
            return thirdApp
        }

        fun createGroup() : DesktopItem {
            val group = DesktopItem()
            group.packageName = "group_" + Random().nextInt(10000)
            group.typeStr = Type.GROUP.toString()
            group.type = Type.GROUP
            group.state = ItemState.Visible
            group.stateVar = 1
            group.label = LauncherConfig.DEFAULT_FOLDER_NAME
            return group
        }

        fun getIntentFromApp(app: App): Intent {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setClassName(app.packageName, app.className)
            return intent
        }

        fun getIntentFromApp(app: DesktopItem): Intent {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setClassName(app.packageName, app.className ?: "")
            return intent
        }
    }

}