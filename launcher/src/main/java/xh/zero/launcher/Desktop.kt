package xh.zero.launcher

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.isNotEmpty
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import xh.zero.launcher.repo.DesktopItem
import xh.zero.launcher.repo.LauncherRepository
import java.util.ArrayList

class Desktop : ViewPager, DesktopCallback {

    private var homePages: List<Fragment> = listOf()
    val appPages: ArrayList<CellContainer> = ArrayList()
    private var desktopAdapter: DesktopAdapter? = null

    private val coordinate = Point(-1, -1)
    private val previousDragPoint = Point()

    private var previousItem: DesktopItem? = null
    private var previousItemView: View? = null
    private var previousPage = 0

    val repo: LauncherRepository by lazy {
        LauncherRepository.instance(context)
    }

    private var appPageNum: Int = 0

    constructor(context: Context) : super(context) {

    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {

    }

    /**
     * 初始化桌面
     */
    fun initial(fm: FragmentManager, homePages: List<Fragment>) {
        this.homePages = homePages
        HOME_PAGE_NUM = homePages.size

        repo.queryAppsCount { appCount ->
            if (appCount > 0) {
                repo.getMaxAppPage { maxPage ->
                    // 加载历史记录，这里需要查询系统App，获取app图标
                    AppLoader.instance(context).getInstalledApps {
                        initialDesktop(fm, maxPage + 1, false)
                    }
                }
            } else {
                // 查询系统App数量，并按页保存App信息
                repo.requestInstalledApps(context) { count ->
                    val pageNum = if (count % PAGE_APP_NUM == 0) count / PAGE_APP_NUM else count / PAGE_APP_NUM + 1
                    initialDesktop(fm, pageNum, true)
                }
            }
        }

    }

    private fun initialDesktop(fm: FragmentManager, pageNum: Int, isNew: Boolean) {
        appPageNum = pageNum

        appPages.clear()
        for (i in 0 until appPageNum) {
            val cellContainer = CellContainer(context)
            cellContainer.setGridSize()
            appPages.add(cellContainer)
        }

        desktopAdapter = DesktopAdapter(fm)
        adapter = desktopAdapter

        for (i in 0 until appPageNum) {
            // 从数据库按每页加载App
            loadAppsForPage(i, appPages[i], isNew)
        }
    }

    fun getTotalPageSize() = adapter?.count ?: 0

    /**
     * 添加右侧新的app页面
     */
    fun addPageRight() {
        val previousPage = currentItem
        desktopAdapter?.addPageRight()
        currentItem = previousPage + 1
//        _pageIndicator.invalidate()
    }

    /**
     * 删除app页面
     */
    fun removePage(position: Int) {
        desktopAdapter?.removePage(position)
    }

    /**
     * 按每页加载App
     */
    private fun loadAppsForPage(position: Int, cellContainer: CellContainer, isNew: Boolean) {
        repo.findAllItemsForPage(position) { items ->
            if (items.isNotEmpty()) {
                if (isNew) {
                    cellContainer.addAll(items)
                } else {
                    cellContainer.addHistory(items)
                }
            }
        }
    }

    /**
     * 保存当前拖动的图标
     */
    override fun setLastItem(item: DesktopItem, view: View?) {
        val currentPage = getCurrentAppPage() ?: return

        previousPage = getCurrentAppPageIndex()
        previousItemView = view
        previousItem = item
        currentPage.removeView(view)
    }

    /**
     * 恢复当前拖动的图标
     */
    override fun revertLastItem() {
        if (previousItemView != null) {
            if (adapter!!.count >= previousPage && previousPage > -1) {
                if (previousPage >= appPages.size) return
                val cellContainer = appPages[previousPage]
                cellContainer.addViewToGrid(previousItemView!!, previousItem!!.x, previousItem!!.y)
                previousItem = null
                previousItemView = null
                previousPage = -1
            }
        }
    }

    override fun consumeLastItem() {
        previousItem = null
        previousItemView = null
        previousPage = -1
    }

    override fun addItemToPoint(item: DesktopItem, x: Int, y: Int): Boolean {
        val cellContainer = getCurrentAppPage() ?: return false
        val positionToLayoutPrams: Point = cellContainer.coordinateToLayoutParams(x, y, Point(item.x, item.y)) ?: return false
        item.x = positionToLayoutPrams.x
        item.y = positionToLayoutPrams.y
        val itemView = ItemViewFactory.getView(context, this, item, null)
        Log.d(CellContainer.TAG, "添加图标到：(${item.x}, ${item.y})")
        cellContainer.addViewToGrid(itemView, item.x, item.y)
        return true
    }

    override fun addItemToPage(item: DesktopItem, page: Int): Boolean {
        val itemView = ItemViewFactory.getView(context, this, item, null)
        val currentPage = getCurrentAppPage() ?: return false
        currentPage.addViewToGrid(itemView, item.x, item.y)
        return true
    }

    override fun addItemToCell(item: DesktopItem, x: Int, y: Int): Boolean {
        val currentPage = getCurrentAppPage() ?: return false
        item.x = x
        item.y = y
        val itemView = ItemViewFactory.getView(context, this, item, null)
//        itemView.setIcon(item.icon)
//        itemView.setLabel(item.label)
//        itemView.setOnLongClickListener(DragHandler.longClick(item, this))
        currentPage.addViewToGrid(itemView, item.x, item.y)
        return true
    }

    override fun removeItem(view: View?, animate: Boolean) {
        val currentPage = getCurrentAppPage() ?: return

        if (animate) {
            view!!.animate().setDuration(100).scaleX(0.0f).scaleY(0.0f).withEndAction {
                if (currentPage == view.parent) {
                    currentPage.removeView(view)
                }
            }
        } else if (currentPage == view!!.parent) {
            currentPage.removeView(view)
        }
    }

    fun updateItem(item: DesktopItem) {
        getCurrentAppPage()?.updateItem(item)
    }

    fun getCurrentAppPage() : CellContainer? {
        return appPages[getCurrentAppPageIndex()]
    }

    fun getCurrentAppPageIndex() = if (currentItem - HOME_PAGE_NUM < 0) 0 else currentItem - HOME_PAGE_NUM

    /**
     * 更新图标投影
     */
    fun updateIconProjection(x: Int, y: Int) {
        val currentPage = getCurrentAppPage() ?: return
        val dragNDropView: ItemOptionView = Launcher.instance().itemOptionView!!
        val state: CellContainer.DragState = currentPage.peekItemAndSwap(x, y, coordinate)
        if (coordinate != previousDragPoint) {
            // 关闭文件夹预览
//            dragNDropView.cancelFolderPreview()
            currentPage.closeFolderPreview()
        }
        previousDragPoint.set(coordinate.x, coordinate.y)
        when (state) {
            // 当前单元格没有被占据，显示图标预览
            CellContainer.DragState.CurrentNotOccupied -> currentPage.projectImageOutlineAt(
                coordinate,
                DragHandler.cachedDragBitmap
            )
            // 当前单元格被占据
            CellContainer.DragState.CurrentOccupied -> {
                for (page in appPages) {
                    page.clearCachedOutlineBitmap()
                }
                // TODO 显示文件夹预览
                currentPage.showFolderPreview(coordinate)
//                dragNDropView.showFolderPreviewAt(
//                    this,
//                    currentPage.cellWidth * (coordinate.x + 0.5f) /*+ resources.getDimension(R.dimen.desktop_padding_start)*/,
//                    currentPage.cellHeight * (coordinate.y + 0.5f) /*+ resources.getDimension(R.dimen.desktop_padding_top)*/
//                )
            }
            CellContainer.DragState.OutOffRange, CellContainer.DragState.ItemViewNotFound -> {
            }
            else -> {
            }
        }
    }

    /**
     * 进入编辑模式
     */
    fun enterDesktopEditMode() {
        val scaleFactor = 0.95f
        val translateFactor = Tool.dp2px(20f).toFloat()
        for (v in appPages) {
            v.animateBackgroundShow()
            val animation =
                v.animate().scaleX(scaleFactor).scaleY(scaleFactor).translationY(translateFactor)
            animation.interpolator = AccelerateDecelerateInterpolator()
        }
    }

    /**
     * 退出编辑模式
     */
    fun exitDesktopEditMode() {
        val scaleFactor = 1.0f
        val translateFactor = 0.0f
        for (v in appPages) {
//            v.setBlockTouch(false)
            v.animateBackgroundHide()
            val animation =
                v.animate().scaleX(scaleFactor).scaleY(scaleFactor).translationY(translateFactor)
            animation.interpolator = AccelerateDecelerateInterpolator()
        }

        getCurrentAppPage()?.closeFolderPreview()
    }

    // --------- App 安装管理 start ----------------
    fun onAppInstall(packageName: String?) {
        if (packageName == null) return
        AppLoader.instance(context).getInstalledApps { apps ->
            for (i in apps.indices) {
                val app = apps[i]
                if (app.packageName == packageName) {
//                    if (Configs.babyBusAppMap.keys.contains(app.packageName)) {
//                        // 幼儿系统页面添加
//                        val page = Configs.babyBusAppMap[app.packageName]
//                        val item = DesktopItem.createApp(app)
//                        item.page = page!!
//                        item.isBabyBus = true
//                        repo.saveItem(item)
//                    } else {
//                        addItem(DesktopItem.createApp(app))
//                    }

                    addItem(DesktopItem.createApp(app))
                    break
                }
            }
        }
    }

    private fun addItem(item: DesktopItem) {
        var hasAdded = false
        for (i in appPages.indices) {
            val page = appPages[i]
            if (page.isNotEmpty()) {
                val pos = page.findFreeSpace()
                if (pos != null) {
                    val itemView = ItemViewFactory.getView(context, this, item, null, true)
                    item.page = i
                    item.x = pos.x
                    item.y = pos.y
                    page.addViewToGrid(itemView, pos.x, pos.y)
                    hasAdded = true
                    break
                }
            }
        }
        if (!hasAdded) {
            addPageRight()
            postDelayed({
                addItem(item)
            }, 200)
        } else {
            repo.saveItem(item)
        }
    }

    fun onAppUninstall(packageName: String?) {
        if (packageName == null) return
        repo.findItem(packageName) { item ->
            if (item != null) {
                removeItem(item)
            }
        }
    }

    private fun removeItem(item: DesktopItem) {
        if (item.stateVar == DesktopItem.ItemState.Hidden.ordinal) {
//            Log.d(TAG, "文件夹卸载: ${item.packageName}, ${item.folderPackageName}")
            // 文件夹内的app删除
            if (item.folderPackageName != null) {
                repo.findGroupItem(item.folderPackageName!!) { group ->
//                    Log.d(TAG, "找到文件夹: ${group?.packageName}, ${group?.x}, ${group?.y}")
                    if (group != null) {
                        group.items.remove(item)
                        repo.saveItem(group) {
                            // 先删除文件夹图标然后重新添加
                            getCurrentAppPage()?.removeView(group.x, group.y)
                            addItemToCell(group, group.x, group.y)
                        }
                    }
                }
            }
        } else {
            // App删除
            getCurrentAppPage()?.removeView(item.x, item.y)
        }
        repo.deleteItem(item, false)
        for (i in appPages.indices) {
            // 删除空白页面
            if (appPages[i].isEmpty()) {
                removePage(i)
            }
        }
    }
    // --------- App 安装管理 end ----------------

    private inner class DesktopAdapter(private val fm: FragmentManager) : PagerAdapter() {

        private val homeContainers = ArrayList<FrameLayout>()

        init {
            for (i in 0 until HOME_PAGE_NUM) {
                val fragContainer = FrameLayout(context)
                fragContainer.id = i + 100
                homeContainers.add(fragContainer)
            }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val v: View
            if (position < HOME_PAGE_NUM) {
                // home 页面
                val fragContainer = homeContainers[position]
                container.addView(fragContainer, LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                val frag = homePages[position]
                fm.beginTransaction()
                    .replace(fragContainer.id, frag)
                    .commit()
                v = fragContainer
            } else {
                // App 页面
                v = appPages[position - HOME_PAGE_NUM]
//                v.setPadding(
//                    resources.getDimension(R.dimen.desktop_padding_start).toInt(),
//                    resources.getDimension(R.dimen.desktop_padding_top).toInt(),
//                    resources.getDimension(R.dimen.desktop_padding_end).toInt(),
//                    resources.getDimension(R.dimen.desktop_padding_bottom).toInt()
//                )
                container.addView(v)
            }
            return v
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj

        override fun getCount(): Int = HOME_PAGE_NUM + appPages.size

//        override fun getItem(position: Int): Fragment {
//            return frags[position]
//        }

        override fun getItemPosition(obj: Any): Int {
            return POSITION_NONE
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return super.getPageTitle(position)
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(obj as View)
        }

        fun addPageRight() {
            val cellContainer = CellContainer(context)
            cellContainer.setGridSize()
            appPages.add(cellContainer)
            notifyDataSetChanged()
        }

        fun removePage(position: Int) {
            appPages.removeAt(position)
            notifyDataSetChanged()
        }
    }

    companion object {
        private var HOME_PAGE_NUM = 0
        const val PAGE_APP_NUM = LauncherConfig.PAGE_APP_NUM

        const val TAG = "Desktop"

        /**
         * dropItem: 正在拖拽的图标
         * item: 想要拖拽放下的单元格中的图标
         * 这里是把item拖拽到dropItem中
         * itemView：放置位置的图标视图 (item的视图)
         * parent：单元格容器
         * page: 第几页
         * callback：这里是Desktop
         */
        fun handleOnDropOver(
            dropItem: DesktopItem?,
            item: DesktopItem?,
            itemView: View?,
            parent: CellContainer,
            page: Int,
            callback: DesktopCallback
        ): Boolean {
            if (item == null || dropItem == null || item.type == null) return false
            val repo = Launcher.instance().repo
            when (item.type) {
                DesktopItem.Type.APP -> {
                    if (dropItem.type == DesktopItem.Type.APP) {
                        // App -> App, 添加App Icon到新的桌面或者当前桌面的文件夹
                        parent.removeView(itemView)
                        // 创建文件夹
                        val group: DesktopItem = DesktopItem.createGroup()
                        group.items.add(item)
                        group.items.add(dropItem)
                        // 把文件夹位置设置为放置拖拽图标的位置(不是坐标)
                        group.x = item.x
                        group.y = item.y
                        // 更新dropItem
                        dropItem.page = page
                        dropItem.stateVar = DesktopItem.ItemState.Hidden.ordinal
                        repo?.saveItem(dropItem)
                        // 更新item
                        item.stateVar = DesktopItem.ItemState.Hidden.ordinal
                        repo?.saveItem(item)
                        // 保存文件夹
                        group.page = page
                        // 更新父级文件夹包名
                        group.items.forEach {
                            it.folderPackageName = group.packageName
                        }
                        repo?.saveItem(group)
                        // 更新桌面
                        callback.addItemToPage(group, page)
                        callback.consumeLastItem()
                    } else if (dropItem.type == DesktopItem.Type.GROUP) {
                        // folder -> app, 添加App Icon到桌面文件夹
                        parent.removeView(itemView)
                        val group: DesktopItem = DesktopItem.createGroup()
                        group.items.add(item)
                        group.items.addAll(dropItem.items)
                        group.x = item.x
                        group.y = item.y
                        // 删除拖拽的文件夹
                        repo?.deleteItem(dropItem, false)
                        item.stateVar = DesktopItem.ItemState.Hidden.ordinal
                        repo?.saveItem(item)
                        // 保存文件夹
                        group.page = page
                        // 更新父级文件夹包名
                        group.items.forEach {
                            it.folderPackageName = group.packageName
                        }
                        repo?.saveItem(group)
                        // 更新桌面
                        callback.addItemToPage(group, page)
                        callback.consumeLastItem()
                        return true
                    }
                }
                DesktopItem.Type.GROUP -> {
                    if (dropItem.type == DesktopItem.Type.APP) {
                        // App -> folder
                        parent.removeView(itemView)
                        item.items.add(dropItem)
                        // 保存拖拽中的图标
                        dropItem.page = page
                        dropItem.stateVar = DesktopItem.ItemState.Hidden.ordinal
                        repo?.saveItem(dropItem)
                        // 更新文件夹图标
                        item.page = page
                        // 更新父级文件夹包名
                        item.items.forEach {
                            it.folderPackageName = item.packageName
                        }
                        repo?.saveItem(item)
                        // 更新桌面
                        callback.addItemToPage(item, page)
                        callback.consumeLastItem()
                        return true
                    } else if (dropItem.type == DesktopItem.Type.GROUP) {
                        // folder -> folder
                        parent.removeView(itemView)
                        // 添加所有拖拽文件夹中的图标
                        item.items.addAll(dropItem.items)
                        // 更新目标文件夹
                        item.page = page
                        // 更新父级文件夹包名
                        item.items.forEach {
                            it.folderPackageName = item.packageName
                        }
                        repo?.saveItem(item)
                        // 更新桌面
                        repo?.deleteItem(dropItem, false)
                        callback.addItemToPage(item, page)
                        callback.consumeLastItem()
                        return true
                    }
                }
            }
            return false
        }
    }
}