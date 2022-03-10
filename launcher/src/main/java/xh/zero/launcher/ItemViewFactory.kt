package xh.zero.launcher

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import xh.zero.launcher.repo.DesktopItem

class ItemViewFactory {
    companion object {
        fun getView(
            context: Context?,
            callback: DesktopCallback?,
            item: DesktopItem,
            showLabel: Boolean? = null,
            hasLongClickEvent: Boolean = true
        ): View? {
            if (context == null) return null
            var view: View? = null

            val builder = AppView.Builder(context)
                .setIconSize(context.resources.getDimension(R.dimen.desktop_app_icon_size))
                .setTargetWidth(context.resources.getDimension(R.dimen.desktop_app_icon_target_size).toInt())
                .withOnLongClick(item, callback)

            when(item.type) {
                DesktopItem.Type.APP -> {
                    view = builder.setAppItem(item).build()
                }
                DesktopItem.Type.GROUP -> {
                    view = builder.setGroupItem(callback, item).build()
                }
            }

            view?.tag = item

            return view
        }
    }
}