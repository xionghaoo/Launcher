package xh.zero.launcher

import android.graphics.PointF
import android.view.View
import xh.zero.launcher.repo.DesktopItem

/**
 * 拖拽事件
 */
interface DropTargetListener {
    val view: View

    /**
     * 顺序1
     */
    fun onStart(location: PointF, isInside: Boolean): Boolean
    /**
     * 顺序2
     */
    fun onStartDrag(location: PointF)

    /**
     * 顺序3
     */
    fun onMove(location: PointF)
    /**
     * 顺序4
     */
    fun onEnter(location: PointF)
    /**
     * 顺序5
     */
    fun onExit(location: PointF)

    /**
     * 顺序6：拖拽完成
     * location: 正在拖拽的图标的坐标
     * item: 正在拖拽的图标
     */
    fun onDrop(location: PointF, item: DesktopItem?)

    /**
     * 顺序6
     * 当图标的坐标在可用范围之外时，拖拽会放置失败
     */
    fun onDropFailure(item: DesktopItem?)

    /**
     * 顺序7
     */
    fun onEnd()
}