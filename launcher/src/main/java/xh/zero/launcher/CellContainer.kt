package xh.zero.launcher

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import xh.zero.launcher.repo.DesktopItem

/**
 * 桌面每一页的app容器
 */
class CellContainer : ViewGroup {

    companion object {
        const val TAG = "CellContainer"
    }

    enum class DragState {
        CurrentNotOccupied, OutOffRange, ItemViewNotFound, CurrentOccupied
    }

    enum class PeekDirection {
        UP, LEFT, RIGHT, DOWN
    }

    var cellWidth: Int = 0
    var cellHeight: Int = 0
    // app icon 单元格数
    private var row: Int = 6
    private var col: Int = 5

    private var cells: ArrayList<ArrayList<Rect>> = arrayListOf()
    private var occupied = ArrayList<ArrayList<Boolean>>()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        alpha = 0
    }
    private var animateBackground = false

    /**
     * 图标投影相关
     */
    private var cachedOutlineBitmap: Bitmap? = null
    private val currentOutlineCoordinate = Point(-1, -1)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0
    }

    private var peekDirection: PeekDirection? = null
    private var peekDownTime = -1L
    private var preCoordinate = Point(-1, -1)
    private var startCoordinate = Point()

    private var isShowFolderPreview = false
    private val folderCoordinate = Point(-1, -1)
    private val folderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 80
    }
    private val folderRect = RectF()

    constructor(context: Context) : super(context) {
        initial(context)
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initial(context)
    }

    private fun initial(context: Context) {
        // 必须要加，不然onDraw方法不会触发
        setWillNotDraw(false)
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
//        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
//        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
//        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
//            setMeasuredDimension(WRAP_WIDTH, WRAP_HEIGHT)
//        } else if (widthMode == MeasureSpec.AT_MOST) {
//            setMeasuredDimension(WRAP_WIDTH, heightSize)
//        } else if (heightMode == MeasureSpec.AT_MOST) {
//            setMeasuredDimension(widthSize, WRAP_HEIGHT)
//        } else {
//            setMeasuredDimension(widthSize, heightSize)
//        }
//    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        //Animating alpha and drawing projected image
        val itemOptionView = Launcher.instance().itemOptionView
        if (itemOptionView != null
            && itemOptionView.dragExceedThreshold
            && currentOutlineCoordinate.x != -1
            && currentOutlineCoordinate.y != -1) {
//            if (outlinePaint.alpha != 160)
//                outlinePaint.alpha = (outlinePaint.alpha + 20).coerceAtMost(160)
            outlinePaint.alpha = 100
            drawCachedOutlineBitmap(
                canvas,
                cells[currentOutlineCoordinate.x][currentOutlineCoordinate.y]
            )
//            if (outlinePaint.alpha <= 160) invalidate()
        }

        if (!animateBackground && bgPaint.alpha != 0) {
            bgPaint.alpha = (bgPaint.alpha - 10).coerceAtLeast(0)
            invalidate()
        } else if (animateBackground && bgPaint.alpha != 100) {
            bgPaint.alpha = (bgPaint.alpha + 10).coerceAtMost(100)
            invalidate()
        }

        // 显示文件夹预览
        if (isShowFolderPreview) {
            val rect = cells[folderCoordinate.x][folderCoordinate.y]
            val iconSize = resources.getDimension(R.dimen._100dp)
            val marginHorizontal = (cellWidth - iconSize) / 2
            var marginVertical = (cellHeight - iconSize) / 2
            if (marginVertical < 0) marginVertical = 0f
            folderRect.set(
                rect.left + marginHorizontal,
                rect.top.toFloat() + marginVertical,
                rect.right - marginHorizontal,
                rect.bottom.toFloat() - marginVertical
            )
            canvas.drawRoundRect(folderRect, 10f, 10f, folderPaint)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l - paddingLeft - paddingRight
        val height = b - t - paddingTop - paddingBottom

        cellWidth = width / row
        cellHeight = height / col
        initialCellRect(paddingLeft, paddingTop, width + paddingLeft, height + paddingTop)

        children.forEach { child ->
            val lp: LayoutParams = child.layoutParams as LayoutParams

            child.measure(
                MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY)
            )

            val rect = cells[lp.x][lp.y]
            child.layout(rect.left, rect.top, rect.right, rect.bottom)
        }
    }

    fun animateBackgroundShow() {
        animateBackground = true
        invalidate()
    }

    fun animateBackgroundHide() {
        animateBackground = false
        invalidate()
    }

    /**
     * 初始化app图标单元格
     */
    private fun initialCellRect(l: Int, t: Int, r: Int, b: Int) {
        cells = arrayListOf()
        // 水平偏移
        var curLeft = l
        var curRight: Int = l + cellWidth
        // 垂直偏移
        var curTop = t
        var curBottom: Int = t + cellHeight

        for (i in 0.until(row)) {
            // 向右偏移
            if (i != 0) {
                curLeft += cellWidth
                curRight += cellWidth
            }
            // 创建列
            val colArr = ArrayList<Rect>()
            for (j in 0.until(col)) {
                // 向下偏移
                if (j != 0) {
                    curTop += cellHeight
                    curBottom += cellHeight
                }
                val rect = Rect(curLeft, curTop, curRight, curBottom)
                colArr.add(rect)
            }

            // 重置垂直偏移
            curTop = l
            curBottom = l + cellHeight
            // 添加列
            cells.add(colArr)
        }
    }

    fun addAll(apps: List<DesktopItem>) {
        removeAllViews()
        apps.forEach { app ->
            val pos = findFreeSpace()
            if (pos != null) {
                app.x = pos.x
                app.y = pos.y
                Launcher.instance().repo?.saveItem(app)
                val itemView = ItemViewFactory.getView(context, Launcher.instance().desktop, app, null)
                addViewToGrid(itemView, app.x, app.y)
            } else {
                Log.d(TAG, "空间不足")
            }
        }
    }

    fun addHistory(apps: List<DesktopItem>) {
        removeAllViews()
        apps.forEach { app ->
            // TODO 其他桌面安装安装应用时，本桌面会检测不到
//            if (app.x == -1 || app.y == -1) {
//                findFreeSpace()?.apply {
//                    app.x = x
//                    app.y = y
//                }
//            }
            if (app.state == DesktopItem.ItemState.Visible) {
                val itemView = ItemViewFactory.getView(context, Launcher.instance().desktop, app, null)
                addViewToGrid(itemView, app.x, app.y)
            }
        }
    }

    /**
     * 寻找空的单元格，x方向优先
     */
    fun findFreeSpace() : Point? {
        for (y in 0 until occupied[0].size) {
            for (x in 0 until occupied.size) {
                // 先遍历x方向
                if (!occupied[x][y]) {
                    return Point(x, y)
                }
            }
        }
        return null
    }

    fun findViewByPosition(x: Int, y: Int) : View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            if (lp.x == x && lp.y == y) {
                return child
            }
        }
//        children.forEach { child ->
//            val lp = child.layoutParams as LayoutParams
//            if (lp.x == x && lp.y == y) {
//                return child
//            }
//        }
        return null
    }

    fun addViewToGrid(v: View?, x: Int, y: Int) {
        if (v == null) return
        v.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            x,
            y
        )
        Log.d(TAG, "添加视图坐标: ${v}, ${x}, ${y}")
        addView(v)
    }

    /**
     * 添加单元格时设置占位
     */
    override fun addView(child: View?) {
        if (child != null) {
            setOccupied(true, child.layoutParams as LayoutParams)
        }
        super.addView(child)
    }

    /**
     * 删除单元格时清除占位
     */
    override fun removeView(view: View?) {
        if (view != null) {
            setOccupied(false, view.layoutParams as LayoutParams)
        }
        Log.d(TAG, "删除视图坐标: ${view}")
        super.removeView(view)
    }

    fun removeView(x: Int, y: Int) {
        var v: View? = null
        children.forEach { child ->
            val lp = child.layoutParams as LayoutParams
            if (lp.x == x && lp.y == y) {
                v = child
            }
        }
//        Log.d(TAG, "删除视图坐标: ${v}, ${x}, ${y}")
        removeView(v)
    }

    fun updateItem(item: DesktopItem) {
        children.forEach { child ->
            val lp = child.layoutParams as LayoutParams
            if (lp.x == item.x && lp.y == item.y) {
                val itemView = child as AppView
                itemView.label = item.label
                itemView.postInvalidate()
            }
        }
    }

    fun setOccupied(b: Boolean, lp: LayoutParams) {
        for (j in 0 until occupied.size) {
            for (i in occupied[j].indices) {
                occupied[lp.x][lp.y] = b
            }
        }
    }

    /**
     * 初始化单元格
     */
    fun setGridSize(x: Int? = null, y: Int? = null) {
        row = x ?: 6
        col = y ?: 5
        occupied = arrayListOf()
        for (i in 0 until row) {
            val colArr = ArrayList<Boolean>()
            for (j in 0 until col) {
                colArr.add(false)
            }
            occupied.add(colArr)
        }
        requestLayout()
    }

    fun showFolderPreview(coordinate: Point) {
        isShowFolderPreview = true
        folderCoordinate.set(coordinate.x, coordinate.y)
        invalidate()
    }

    fun closeFolderPreview() {
        isShowFolderPreview = false
        invalidate()
    }

    /**
     * 绘制图标投影
     */
    fun projectImageOutlineAt(newCoordinate: Point, bitmap: Bitmap?) {
        cachedOutlineBitmap = bitmap
        if (currentOutlineCoordinate != newCoordinate) {
            outlinePaint.alpha = 0
        }
        currentOutlineCoordinate.set(newCoordinate.x, newCoordinate.y)
        invalidate()
    }

    private fun drawCachedOutlineBitmap(canvas: Canvas, cell: Rect) {

        if (cachedOutlineBitmap != null) {
            val bitmap: Bitmap = cachedOutlineBitmap!!
            val centerX = cell.centerX().toFloat()
            val centerY = cell.centerY().toFloat()
            canvas.drawBitmap(
                bitmap,
                centerX - bitmap.width / 2,
                centerY - bitmap.height / 2,
                outlinePaint
            )
        }
    }

    fun peekItemAndSwap(x: Int, y: Int, coordinate: Point): DragState {
        touchPosToCoordinate(coordinate, x, y, checkAvailability = false, checkBoundary = false)
        if (coordinate.x != -1 && coordinate.y != -1) {
            // 得到起点的单元格位置
            startCoordinate = coordinate

            // 如果上一个位置不是当前位置，重置peek时间
            if (preCoordinate != coordinate) {
                peekDownTime = -1L
            }
            if (peekDownTime == -1L) {
                // 获取进入单元格的方向，这里拖动的icon已经进入当前单元格了
                peekDirection = getPeekDirectionFromCoordinate(startCoordinate, coordinate)
                // 更新peek时间
                peekDownTime = System.currentTimeMillis()
                // 记录当前peek的位置
                preCoordinate = coordinate
            }
            // 检查当前单元格是否被占据
            return if (occupied[coordinate.x][coordinate.y]) {
                DragState.CurrentOccupied
            } else {
                DragState.CurrentNotOccupied
            }
        }
        return DragState.OutOffRange
    }

    fun coordinateToLayoutParams(
        mX: Int,
        mY: Int,
        point: Point? = null
    ): Point? {
        val pos = point ?: Point()
        touchPosToCoordinate(pos, mX, mY, true)
        return if (!pos.equals(-1, -1)) pos else null
    }

    fun touchPosToCoordinate(
        coordinate: Point,
        mX: Int,
        mY: Int,
        checkAvailability: Boolean
    ) {
        touchPosToCoordinate(coordinate, mX, mY, checkAvailability, false)
    }

    /**
     * 把位置转成坐标
     */
    fun touchPosToCoordinate(
        coordinate: Point,
        ox: Int,
        oy: Int,
        checkAvailability: Boolean,
        checkBoundary: Boolean
    ) {
        var tx = ox
        var ty = oy
        if (cells.size == 0) {
            coordinate.set(-1, -1)
            return
        }
//        mX -= ((xSpan - 1) * _cellWidth / 2f).toInt()
//        mY -= ((ySpan - 1) * _cellHeight / 2f).toInt()
        var x = 0
        while (x < row) {
            var y = 0
            while (y < col) {
                // 转换
                val cell: Rect = cells[x][y]
                // 如果移动中触摸点的坐标在单元格内，设置当前单元格的位置
                if (ty >= cell.top && ty <= cell.bottom && tx >= cell.left && tx <= cell.right) {
                    // 检查单元格的可用性
                    if (checkAvailability) {
                        if (occupied[x][y]) {
                            coordinate.set(-1, -1)
                            Log.e(TAG, "checkAvailability: 单元格不可用1")
                            return
                        }
                        var dx = x
                        var dy = y
                        if (dx >= row - 1) {
                            dx = row - 1
                            x = dx
                        }
                        if (dy >= col - 1) {
                            dy = col - 1
                            y = dy
                        }
                        for (x2 in x until x + 1) {
                            for (y2 in y until y + 1) {
                                if (occupied[x2][y2]) {
                                    coordinate.set(-1, -1)
                                    Log.e(TAG, "checkAvailability: 单元格不可用2")
                                    return
                                }
                            }
                        }
                    }
                    // 检查边界条件
                    if (checkBoundary) {
                        val offsetCell = Rect(cell)
                        val dp2 = resources.getDimension(R.dimen._6dp).toInt()
                        offsetCell.inset(dp2, dp2)
                        Log.e(TAG, "checkBoundary: ${cell}, (${offsetCell.top}, ${offsetCell.bottom}, ${offsetCell.left}, ${offsetCell.right}), $tx, $ty")
                        if (ty >= offsetCell.top && ty <= offsetCell.bottom && tx >= offsetCell.left && tx <= offsetCell.right) {
                            coordinate.set(-1, -1)
                            Log.e(TAG, "checkBoundary: 边界条件不满足")
                            return
                        }
                    }
                    // 设置单元格的位置
                    coordinate.set(x, y)
                    return
                }
                y++
            }
            x++
        }
    }

    private fun getPeekDirectionFromCoordinate(from: Point, to: Point): PeekDirection? {
        if (from.y - to.y > 0) {
            return PeekDirection.UP
        }
        if (from.y - to.y < 0) {
            return PeekDirection.DOWN
        }
        if (from.x - to.x > 0) {
            return PeekDirection.LEFT
        }
        return if (from.x - to.x < 0) {
            PeekDirection.RIGHT
        } else null
    }

    fun clearCachedOutlineBitmap() {
        outlinePaint.alpha = 0
        cachedOutlineBitmap = null
        isShowFolderPreview = false
        invalidate()
    }

    /**
     * 根据位置找出单元格视图
     */
    fun coordinateToChildView(pos: Point?): View? {
        if (pos == null) {
            return null
        }
        for (i in 0 until childCount) {
            val lp: LayoutParams = getChildAt(i).layoutParams as LayoutParams
            if (pos.x >= lp.x && pos.y >= lp.y
                && pos.x < lp.x + 1 && pos.y < lp.y + 1) {
                return getChildAt(i)
            }
        }
        return null
    }

    fun isEmpty(): Boolean = childCount == 0

    class LayoutParams : ViewGroup.LayoutParams {
        var x = 0
        var y = 0

        constructor(w: Int, h: Int, x: Int, y: Int) : super(w, h) {
            this.x = x
            this.y = y
        }

        constructor(w: Int, h: Int) : super(w, h) {}
    }
}