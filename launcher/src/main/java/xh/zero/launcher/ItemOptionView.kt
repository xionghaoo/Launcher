package xh.zero.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import xh.zero.launcher.repo.DesktopItem
import java.util.HashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ACTION_DOWN = 0
 * ACTION_UP = 1
 * ACTION_MOVE = 2
 */
open class ItemOptionView : FrameLayout {

    private var isDragging: Boolean = false
    // 拖拽开始的位置
    private var dragLocation = PointF()
    private var dragLocationConverted = PointF()

    private val overlayView = OverlayView(context)
    private val registeredDropTargetEntries = LinkedHashMap<DropTargetListener, DragFlag>()
    private var dragItem: DesktopItem? = null

    private val tempArrayOfInt2 = IntArray(2)
    var dragExceedThreshold = false
    private var dragLocationStart = PointF()

    private var dragView: View? = null
    private var overlayPopupShowing: Boolean = false
    private var overlayIconScale = 0f

    private var iconPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)


    class DragFlag {
        var previousOutside = true
        var shouldIgnore = false
    }

    constructor(context: Context) : super(context) {

    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        addView(overlayView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        overlayView.bringToFront()
//        overlayPopup.bringToFront()
    }

    private inner class OverlayView : View {
        constructor(context: Context) : super(context) {
            setWillNotDraw(false)
        }

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event == null || event.actionMasked != MotionEvent.ACTION_DOWN || isDragging || !overlayPopupShowing) {
                return super.onTouchEvent(event)
            }
            collapse()
            return true
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            if (canvas == null || DragHandler.cachedDragBitmap == null || dragLocation.equals(-1f, -1f))
                return
//
            val x = dragLocation.x
            val y = dragLocation.y
//
            if (isDragging) {
                canvas.save()
                // 在overlay上绘制被拖动的icon，拖动icon时放大一点图标
                overlayIconScale = max(1f, min(1.1f, overlayIconScale + 0.05f))
                canvas.scale(
                    overlayIconScale,
                    overlayIconScale,
                    x + DragHandler.cachedDragBitmap!!.width / 2,
                    y + DragHandler.cachedDragBitmap!!.height / 2
                )
                canvas.drawBitmap(
                    DragHandler.cachedDragBitmap!!,
                    x - DragHandler.cachedDragBitmap!!.width / 2,
                    y - DragHandler.cachedDragBitmap!!.height / 2,
                    iconPaint
                )
                canvas.restore()
            }
//
            if (isDragging) invalidate()
        }
    }

    // 拦截事件
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && ev.actionMasked == MotionEvent.ACTION_UP && isDragging) {
            // 手指抬起
            handleDragFinished()
        }
        if (isDragging) {
            return true
        }
        if (ev != null) {
            dragLocation.set(ev.x, ev.y)
        }
        return super.onInterceptTouchEvent(ev)

    }

    // 处理事件
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            if (isDragging) {
                dragLocation.set(event.x, event.y)
                when (event.actionMasked) {
                    // ACTION_UP
                    MotionEvent.ACTION_UP -> handleDragFinished()
                    // ACTION_MOVE
                    MotionEvent.ACTION_MOVE -> handleMovement()
                    else -> {
                    }
                }
                if (isDragging) {
                    return true
                }
                return super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)

    }

    /**
     * 开始拖拽时调用，把DesktopItem传进来
     */
    fun startDragNDropOverlay(view: View, item: DesktopItem) {
        isDragging = true
        dragExceedThreshold = false
        overlayIconScale = 0.0f
        dragView = view
        dragItem = item

        dragLocationStart.set(dragLocation)
        for ((key, value) in registeredDropTargetEntries.entries) {
            convertPoint(key.view)
            val dragFlag: DragFlag = value
            val dropTargetListener: DropTargetListener = key
            // 发送初始化事件，返回false表示不处理拖拽事件
            dragFlag.shouldIgnore = !dropTargetListener.onStart(dragLocationConverted, isViewContains(key.view, dragLocation.x.toInt(), dragLocation.y.toInt()))
        }
        overlayView.invalidate()
    }

    fun registerDropTarget(targetListener: DropTargetListener) {
        registeredDropTargetEntries[targetListener] = DragFlag()
    }

    /**
     * 拖拽中
     */
    private fun handleMovement() {
        // 判断拖拽移动的距离，超过拖拽阈值才发送拖拽事件
        if (!dragExceedThreshold
            && (abs(dragLocationStart.x - dragLocation.x) > DRAG_THRESHOLD
            || abs(dragLocationStart.y - dragLocation.y) > DRAG_THRESHOLD)) {
            dragExceedThreshold = true
            for ((key, value) in registeredDropTargetEntries.entries) {
                if (!value.shouldIgnore) {
                    convertPoint(key.view)
                    val dropTargetListener: DropTargetListener = key as DropTargetListener
                    // 发送拖拽开始事件
                    dropTargetListener.onStartDrag(dragLocationConverted)
                }
            }
        }
        if (dragExceedThreshold) {
            // 开始拖拽时清除视图
            collapse()
        }
        for (target in registeredDropTargetEntries.entries) {
            var dropTargetListener: DropTargetListener = target.key
            if (!target.value.shouldIgnore) {
                convertPoint(target.key.view)
                if (isViewContains(target.key.view, dragLocation.x.toInt(), dragLocation.y.toInt())) {
                    // 发送拖拽移动事件
                    dropTargetListener.onMove(dragLocationConverted)
                    if (target.value.previousOutside) {
                        target.value.previousOutside = false
                        dropTargetListener = target.key
                        // 发送拖拽进入视图事件
                        dropTargetListener.onEnter(dragLocationConverted)
                    }
                } else if (!target.value.previousOutside) {
                    target.value.previousOutside = true
                    dropTargetListener = target.key
                    // 发送拖拽退出视图事件
                    dropTargetListener.onExit(dragLocationConverted)
                }
            }
        }
    }

    /**
     * 拖拽完成
     */
    private fun handleDragFinished() {
        Log.d(CellContainer.TAG, "handleDragFinished")
        isDragging = false
        for ((key, value) in registeredDropTargetEntries.entries) {
            if (!value.shouldIgnore) {
                if (isViewContains(key.view, dragLocation.x.toInt(), dragLocation.y.toInt())) {
                    convertPoint(key.view)
                    val dropTargetListener: DropTargetListener = key as DropTargetListener
                    // 发送拖拽完成事件
                    dropTargetListener.onDrop(dragLocationConverted, dragItem)
                } else {
                    val dropTargetListener: DropTargetListener = key as DropTargetListener
                    dropTargetListener.onDropFailure(dragItem)
                }
            }
        }

        for ((key) in registeredDropTargetEntries.entries) {
            // 发送结束事件
            key.onEnd()
        }

//        cancelFolderPreview()
    }

    /**
     * 判断(rx, ry)是否在view的矩形内
     */
    private fun isViewContains(view: View, rx: Int, ry: Int): Boolean {
        view.getLocationOnScreen(tempArrayOfInt2)
        val x: Int = tempArrayOfInt2[0]
        val y: Int = tempArrayOfInt2[1]
        val w = view.width
        val h = view.height
        return !(rx < x || rx > x + w || ry < y || ry > y + h)
    }

    fun convertPoint(toView: View) {
        val fromCoordinate = IntArray(2)
        val toCoordinate = IntArray(2)
        // 获取ItemOptionView的原点
        getLocationOnScreen(fromCoordinate)
        // 获取toView的原点
        toView.getLocationOnScreen(toCoordinate)
        // 获取相对于ItemOptionView的拖拽坐标
        dragLocationConverted.set(
            (fromCoordinate[0] - toCoordinate[0]).toFloat() + dragLocation.x,
            (fromCoordinate[1] - toCoordinate[1]).toFloat() + dragLocation.y
        )
    }

    fun collapse() {
        if (overlayPopupShowing) {
            overlayPopupShowing = false
//            _overlayPopup.animate().alpha(0.0f).withEndAction {
//                _overlayPopup.visibility = INVISIBLE
//                _overlayPopupAdapter.clear()
//            }
            if (!isDragging) {
                dragView = null
                dragItem = null
//                _dragAction = null
            }
        }
    }

    companion object {
        private const val DRAG_THRESHOLD = 20f
    }
}