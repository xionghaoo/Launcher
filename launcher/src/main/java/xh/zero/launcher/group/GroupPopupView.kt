package xh.zero.launcher.group

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import xh.zero.launcher.*
import xh.zero.launcher.repo.DesktopItem
import xh.zero.launcher.repo.LauncherRepository

/**
 * 桌面文件夹
 */
class GroupPopupView : FrameLayout {
    
    private var popupCard: View? = null
    private var vp: ViewPager? = null
    private var textViewGroupName: TextView? = null
    private var edtGroupName: EditText? = null
    private var isShowing: Boolean = false

    private var desktopItem: DesktopItem? = null

    constructor(context: Context) : super(context) {
        initial()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initial()
    }

    private fun initial() {
        popupCard = LayoutInflater.from(context).inflate(R.layout.launcher_popup_view, null)
        vp = popupCard?.findViewById(R.id.launcher_vp_group)
        bringToFront()

        setOnClickListener {
            collapse()
        }

        addView(popupCard)
        val lp = popupCard!!.layoutParams as FrameLayout.LayoutParams
        lp.gravity = Gravity.CENTER
        lp.width = resources.getDimension(R.dimen.folder_width).toInt()
        lp.height = resources.getDimension(R.dimen.folder_width).toInt()
        popupCard?.visibility = View.INVISIBLE
        visibility = View.INVISIBLE
        textViewGroupName = popupCard?.findViewById(R.id.launcher_group_popup_label)
        edtGroupName = popupCard?.findViewById(R.id.launcher_edt_group_popup_label)
        textViewGroupName?.setOnClickListener {
            edtGroupName?.visibility = View.VISIBLE
            edtGroupName?.requestFocus()
        }
        setWillNotDraw(false)
    }

    private fun collapse() {
        if (edtGroupName?.visibility == View.VISIBLE) {
            edtGroupName?.visibility = View.INVISIBLE
            SystemUtil.hideSoftKeyboard(context, edtGroupName!!)

            val label = edtGroupName?.text?.toString()
            textViewGroupName?.text = label
            desktopItem?.label = label
            if (desktopItem != null) {
                Launcher.instance().repo?.saveItem(desktopItem!!)
                Launcher.instance().desktop?.updateItem(desktopItem!!)
            }
        } else {
            vp?.adapter = null
            popupCard?.visibility = View.INVISIBLE
            visibility = View.INVISIBLE
            isShowing = false
            desktopItem = null
        }
    }

    fun showPopup(item: DesktopItem, itemView: View, callback: DesktopCallback?): Boolean {
        if (isShowing || visibility == View.VISIBLE) return false
        isShowing = true
        desktopItem = item
        setBackgroundColor(resources.getColor(R.color.overlay_delete))
        val label = item.label
        textViewGroupName?.visibility = if (label?.isNotEmpty() == true) VISIBLE else GONE
        textViewGroupName?.setText(label)
        edtGroupName?.setText(label)
        edtGroupName?.setSelection(label?.length ?: 0)
        textViewGroupName?.setTextColor(Color.BLACK)
        textViewGroupName?.setTypeface(null, Typeface.BOLD)

        val context = itemView.context

        val tmpPageNum = item.items.size / 9
        val pageNum = if (item.items.size % 9 == 0) tmpPageNum else tmpPageNum + 1
        val itemMap = HashMap<Int, ArrayList<DesktopItem>>()
        item.items.forEachIndexed { i, desktopItem ->
            // 计算当前页  3x3为一页
            val index = i + 1
            var page = if (index % 9 == 0) index / 9 else index / 9 + 1
            if (index == 0) page = 1

            // 计算每页的图标位置
            desktopItem.x = (index - 1) % 9 % 3
            desktopItem.y = (index - 1) % 9 / 3

            // 添加每页图标
            if (itemMap[page] == null) {
                itemMap[page] = ArrayList()
            }
            itemMap[page]?.add(desktopItem)
        }
        vp?.adapter = GroupItemAdapter(context, pageNum, itemMap) { cellContainer, desktopItems ->
            // 遍历文件夹的每一个图标
            desktopItems.forEach { groupItem ->
                val view = ItemViewFactory.getView(getContext(), callback, groupItem, false)
                view?.setOnLongClickListener {
                    removeItem(context, item, groupItem, itemView as AppView)
                    // 拖动事件 - 文件夹内的图标
                    DragHandler.startDrag(view, groupItem, null, item)
                    // 关闭文件夹
                    collapse()

                    // update group icon or
                    // convert group item into app item if there is only one item left
                    updateItem(callback, item, itemView)
                    true
                }
                view?.setOnClickListener {
                    Tool.createScaleInScaleOutAnim(view) {
                        collapse()
                        visibility = INVISIBLE
                        view.context.startActivity(groupItem.intent)
                    }
                }
                cellContainer.addViewToGrid(view, groupItem.x, groupItem.y)
            }
        }

//        val cellSize = GroupDef.getCellSize(item.items.size)
//        cellContainer?.setGridSize(x = cellSize[0], y = cellSize[1])
        val iconSize = resources.getDimension(R.dimen.desktop_app_icon_size).toInt()
        val textSize = resources.getDimension(R.dimen.desktop_app_fold_text_size).toInt()
        val contentPadding = resources.getDimension(R.dimen._6dp).toInt()

        visibility = View.VISIBLE
        popupCard?.visibility = View.VISIBLE

        return true
    }

    /**
     * 把拖出去的图标从文件夹删除
     */
    private fun removeItem(
        context: Context,
        currentItem: DesktopItem,
        dragOutItem: DesktopItem,
        currentView: AppView
    ) {
        val repo = Launcher.instance().repo
        // 文件夹删除拖出去的item
        currentItem.items.remove(dragOutItem)
        dragOutItem.stateVar = DesktopItem.ItemState.Visible.ordinal
        repo?.saveItem(dragOutItem)
        repo?.saveItem(currentItem)
        // 更新桌面的文件夹图标，减掉了一个icon
        currentView.icon = GroupDrawable(context, currentItem, resources.getDimension(R.dimen.desktop_app_icon_size).toInt())
    }

    /**
     * 恢复文件夹
     */
    fun revertItem(
        folderItem: DesktopItem,
        dragOutItem: DesktopItem,
        desktop: Desktop?
    ) {
        val repo = Launcher.instance().repo

        folderItem.items.add(dragOutItem)
        dragOutItem.stateVar = DesktopItem.ItemState.Hidden.ordinal
        repo?.saveItem(dragOutItem)
        repo?.saveItem(folderItem) {
            // 先删除文件夹图标，然后再添加
            desktop?.getCurrentAppPage()?.removeView(folderItem.x, folderItem.y)
            desktop?.addItemToCell(folderItem, folderItem.x, folderItem.y)
        }
    }

    /**
     * 更新文件夹图标
     * currentItem: 当前文件夹图标数据
     * currentView: 当前文件夹图标视图
     */
    private fun updateItem(callback: DesktopCallback?, currentItem: DesktopItem, currentView: View?) {
        if (currentItem.items.size == 0) {
            // TODO 处理文件夹关闭
            val repo = Launcher.instance().repo
//            val app = AppLoader.instance(context).findApp(currentItem.items[0].intent)
//            if (app != null) {
//                repo?.findItem(currentItem.items[0].packageName) { r ->
//                    if (r != null) {
//                        val item: DesktopItem = r
//                        item.x = currentItem.x
//                        item.y = currentItem.y
//
//                        // update db
//                        item.page = Launcher.desktop!!.getCurrentAppPageIndex()
//                        item.stateVar = DesktopItem.ItemState.Visible.ordinal
//                        repo.saveItem(item)
//                        repo.deleteItem(currentItem, false)
//                        repo.recoverSingleItem(item) { rItem ->
//                            // update launcher
//                            callback?.removeItem(currentView, false)
//                            callback?.addItemToCell(rItem, rItem.x, rItem.y)
//                        }
//                    }
//                }
//
//            }
            // 文件夹被清空时删除整个文件夹
            repo?.deleteItem(currentItem, false)
            callback?.removeItem(currentView, false)
        } else {
            callback?.removeItem(currentView, false)
            callback?.addItemToCell(currentItem, currentItem.x, currentItem.y)
        }
    }

    internal object GroupDef {
        var PAGE_APP_NUM = 9
        fun getCellSize(count: Int): IntArray {
            if (count <= 1) return intArrayOf(1, 1)
            if (count <= 2) return intArrayOf(2, 1)
            if (count <= 4) return intArrayOf(2, 2)
            if (count <= 6) return intArrayOf(3, 2)
            if (count <= 9) return intArrayOf(3, 3)
            return if (count <= 12) intArrayOf(4, 3) else intArrayOf(0, 0)
        }
    }


}