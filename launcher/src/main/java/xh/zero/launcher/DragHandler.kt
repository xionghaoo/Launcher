package xh.zero.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.os.Handler
import android.util.Log
import android.view.View
import xh.zero.launcher.repo.DesktopItem
import xh.zero.launcher.widgets.DeleteOptionView

class DragHandler {
    companion object {

        private const val TAG = "DragHandler"
        var cachedDragBitmap: Bitmap? = null

        private var isEnterDeleteMode = false

        private var desktopFolderItem: DesktopItem? = null
        private var groupItem: DesktopItem? = null

        private var isDragFolder = false

        fun startDrag(v: View, app: DesktopItem, desktopCallback: DesktopCallback?, folderItem: DesktopItem?) {
            // 不为空拖拽的是文件夹
            isDragFolder = app.items.isNotEmpty()
//            _cachedDragBitmap = ImageUtil.loadBitmapFromView(v)
            cachedDragBitmap = loadBitmapFromView(v)
            // 开始拖拽事件
            Launcher.instance().itemOptionView?.startDragNDropOverlay(v, app)
            desktopCallback?.setLastItem(app, v)

            // 保存文件夹信息
            if (folderItem != null) {
                Log.d(CellContainer.TAG, "保存文件夹信息： ${folderItem}")
                desktopFolderItem = folderItem
                groupItem = app
            }
        }

        fun longClick(app: DesktopItem, desktopCallback: DesktopCallback?) : View.OnLongClickListener {
            return View.OnLongClickListener { v ->
                startDrag(v, app, desktopCallback, null)
                true
            }
        }

        private fun loadBitmapFromView(view: View): Bitmap? {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            var tempLabel: String? = null
            if (view is AppView) {
                tempLabel = view.label
                view.label = " "
            }
            view.layout(0, 0, view.width, view.height)
            view.draw(canvas)
            if (view is AppView) {
                view.label = tempLabel
            }
            view.parent.requestLayout()
            return bitmap
        }
    }

    val dragHandler = Handler()

    fun initDragNDrop(
        dragNDropView: ItemOptionView,
        leftDragHandle: View,
        rightDragHandle: View,
        topDragHandler: View,
        desktop: Desktop,
        deleteOptionView: DeleteOptionView
    ) {
        // 左边缘处理
        dragNDropView.registerDropTarget(object : DropTargetListener {
            var runnable: Runnable = object : Runnable {
                override fun run() {
                    val i: Int = desktop.getCurrentAppPageIndex()
                    if (i > 0) {
                        desktop.currentItem = desktop.currentItem - 1
                    } else if (i <= 0) {
//                        _homeActivity.getDesktop().addPageLeft(true);
                    }
                    dragHandler.postDelayed(this, 1000)
                }
            }

            override val view: View = leftDragHandle

            override fun onStart(location: PointF, isInside: Boolean): Boolean {
                return true
            }

            override fun onStartDrag(location: PointF) {
                leftDragHandle.animate().alpha(0.5f)
            }

            override fun onDrop(location: PointF, item: DesktopItem?) {

            }

            override fun onDropFailure(item: DesktopItem?) {

            }

            override fun onMove(location: PointF) {

            }

            override fun onEnter(location: PointF) {
                dragHandler.post(runnable)
                leftDragHandle.animate().alpha(0.9f)
            }

            override fun onExit(location: PointF) {
                dragHandler.removeCallbacksAndMessages(null)
                leftDragHandle.animate().alpha(0.5f)
            }

            override fun onEnd() {
                dragHandler.removeCallbacksAndMessages(null)
                leftDragHandle.animate().alpha(0f)
            }
        })

        // 右边缘处理
        dragNDropView.registerDropTarget(object : DropTargetListener {
            var runnable: Runnable = object : Runnable {
                override fun run() {
                    val i: Int = desktop.currentItem
                    if (i < desktop.getTotalPageSize() - 1) {
                        // 移动到新的页面
                        desktop.currentItem = desktop.currentItem + 1
                    } else if (i == desktop.getTotalPageSize() - 1) {
                        // 添加新的页面
                        if (!desktop.getCurrentAppPage()!!.isEmpty()) {
                            desktop.addPageRight()
                        }
                    }
                    dragHandler.postDelayed(this, 1000)
                }
            }

            override val view: View = rightDragHandle

            override fun onStart(location: PointF, isInside: Boolean): Boolean {
                return true
            }

            override fun onStartDrag(location: PointF) {
                rightDragHandle.animate().alpha(0.5f)
            }

            override fun onDrop(location: PointF, item: DesktopItem?) {
            }

            override fun onDropFailure(item: DesktopItem?) {

            }

            override fun onMove(location: PointF) {
            }

            override fun onEnter(location: PointF) {
                dragHandler.post(runnable)
                rightDragHandle.animate().alpha(0.9f)
            }

            override fun onExit(location: PointF) {
                dragHandler.removeCallbacksAndMessages(null)
                rightDragHandle.animate().alpha(0.5f)
            }

            override fun onEnd() {
                dragHandler.removeCallbacksAndMessages(null)
                rightDragHandle.animate().alpha(0f)
            }
        })

        // 顶部拖拽处理
        dragNDropView.registerDropTarget(object : DropTargetListener {

            override val view: View
                get() = topDragHandler

            override fun onStart(location: PointF, isInside: Boolean): Boolean {
                return !isDragFolder
            }

            override fun onStartDrag(location: PointF) {
                isEnterDeleteMode = false
                topDragHandler.animate().alpha(0.5f)
            }

            override fun onDrop(location: PointF, item: DesktopItem?) {
                // 放下，显示删除选项
                desktop.animate().translationY(0f)

                deleteOptionView.visibility = View.VISIBLE
                deleteOptionView.setAppInfo(item?.label, item?.icon)
                deleteOptionView.setOnCancelListener {
                    deleteOptionView.visibility = View.GONE
                    if (desktopFolderItem != null && groupItem != null) {
                        Launcher.instance().groupPopup?.revertItem(desktopFolderItem!!, groupItem!!, desktop)
                        desktopFolderItem = null
                        groupItem = null
                    } else {
                        desktop.revertLastItem()
                    }
                    isEnterDeleteMode = false
                }
                deleteOptionView.setOnConfirmListener {
                    deleteOptionView.visibility = View.GONE
                    // TODO 在交给系统删除前，先恢复图标，
                    // 万一用户在系统对话框取消还可以看到没有删除的app，
                    // 如果后期Launcher有静默删除权限，那就不需要恢复了
                    if (desktopFolderItem != null && groupItem != null) {
                        Launcher.instance().groupPopup?.revertItem(desktopFolderItem!!, groupItem!!, desktop)
                        desktopFolderItem = null
                        groupItem = null
                    } else {
                        desktop.revertLastItem()
                    }
                    SystemUtil.uninstallApp(view.context, item?.packageName)

                    isEnterDeleteMode = false
                }
            }

            override fun onDropFailure(item: DesktopItem?) {
                // 放下
                desktop.animate().translationY(0f)
                Log.d(CellContainer.TAG, "top drag item onDropFailure")

            }

            override fun onMove(location: PointF) {
                isEnterDeleteMode = true
            }

            override fun onEnter(location: PointF) {
                isEnterDeleteMode = true
                // 进入删除模式
                rightDragHandle.animate().alpha(0.9f)
                desktop.animate().translationY(100f)
            }

            override fun onExit(location: PointF) {
                isEnterDeleteMode = false
                // 退出删除模式
                rightDragHandle.animate().alpha(0.5f)
                desktop.animate().translationY(0f)
            }

            override fun onEnd() {
//                isEnterDeleteMode = false
                topDragHandler.animate().alpha(0f)
            }
        })

        // 桌面手势处理
        dragNDropView.registerDropTarget(object : DropTargetListener {

            override val view: View get() = desktop

            override fun onStart(location: PointF, isInside: Boolean): Boolean {
//                dragNDropView.showItemPopup()
                desktop.enterDesktopEditMode()
                return true
            }

            override fun onStartDrag(location: PointF) {
            }

            override fun onDrop(location: PointF, item: DesktopItem?) {
                if (isEnterDeleteMode) return
                Log.d(CellContainer.TAG, "onDrop")
                // 从文件夹拖出图标时清除文件夹记录
                desktopFolderItem = null
                groupItem = null

                val repo = Launcher.instance().repo
                val x = location.x.toInt()
                val y = location.y.toInt()
                if (desktop.addItemToPoint(item!!, x, y)) {
                    // 放置目标处没有图标
                    Log.d(CellContainer.TAG, "放置图标成功")
                    // 更新桌面
                    desktop.consumeLastItem()
                    // 保存放置成功的图标
                    item.page = desktop.getCurrentAppPageIndex()
                    repo?.saveItem(item)
                } else {
                    // 放置目标处存在图标
                    Log.e(CellContainer.TAG, "放置图标失败")
                    val pos = Point()
                    val currentPage = desktop.getCurrentAppPage()
                    if (currentPage != null) {
                        currentPage.touchPosToCoordinate(pos, x, y, false)
                        val itemView: View? = currentPage.coordinateToChildView(pos)

                        if (itemView != null && Desktop.handleOnDropOver(
                                item,
                                itemView.tag as DesktopItem,
                                itemView,
                                desktop.getCurrentAppPage()!!,
                                desktop.getCurrentAppPageIndex(),
                                desktop
                            )
                        ) {
                            desktop.consumeLastItem()
                        } else {
//                            Timber.d("没有足够的空间")
                            desktop.revertLastItem()
                        }
                    }
                }
            }

            override fun onDropFailure(item: DesktopItem?) {
                if (isEnterDeleteMode) return
                desktop.revertLastItem()
            }

            override fun onMove(location: PointF) {
//                if (isEnterDeleteMode) return
                desktop.updateIconProjection(location.x.toInt(), location.y.toInt());
            }

            override fun onEnter(location: PointF) {
            }

            override fun onExit(location: PointF) {
                // 尝试恢复没有放置成功的图标
//                desktop.revertLastItem()

                Log.d(CellContainer.TAG, "onExit")
                // 清除桌面的图标投影
                for (page in desktop.appPages) {
                    page.clearCachedOutlineBitmap()
                }
            }

            override fun onEnd() {
                Log.d(CellContainer.TAG, "onEnd")
                desktop.exitDesktopEditMode()
                for (i in desktop.appPages.indices) {
                    val page: CellContainer = desktop.appPages[i]
                    page.clearCachedOutlineBitmap()
                    // 退出编辑模式后再清空页面
                    if (isEnterDeleteMode) return
                    // 删除空白页面
                    if (page.isEmpty()) {
                        desktop.removePage(i)
                    }
                }
//                Timber.d("onEnd")

                // 恢复拖拽标志
                isDragFolder = false
            }
        })
    }
}