package xh.zero.launcher

import android.view.View
import xh.zero.launcher.repo.DesktopItem

interface DesktopCallback {

    fun setLastItem(item: DesktopItem, view: View?)

    fun revertLastItem()

    fun consumeLastItem()

    fun addItemToPoint(item: DesktopItem, x: Int, y: Int): Boolean

    fun addItemToPage(item: DesktopItem, page: Int): Boolean

    fun addItemToCell(item: DesktopItem, x: Int, y: Int): Boolean

    fun removeItem(view: View?, animate: Boolean)
}