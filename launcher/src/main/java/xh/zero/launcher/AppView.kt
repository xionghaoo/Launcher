package xh.zero.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import xh.zero.launcher.group.GroupDrawable
import xh.zero.launcher.repo.DesktopItem
import java.lang.Exception
import kotlin.math.ceil

/**
 * 桌面图标
 */
class AppView : View, Drawable.Callback {

    var icon: Drawable? = null
    var label: String? = null

    private val textPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = resources.getDimension(R.dimen.desktop_app_icon_label_size)
            color = ContextCompat.getColor(context, R.color.launcher_color_app_icon_label)
        }
    }
    private val textContainer = Rect()

    private var iconSize = 0f
    private val labelHeight: Float = resources.getDimension(R.dimen._20dp)
    var drawIconTop = 0f
        private set
    var showLabel = true
        private set
    private var targetedWidth = 0
    private var targetedHeightPadding = 0

    private var minIconTextMargin: Int = resources.getDimension(R.dimen._4dp).toInt()

    private var notificationCount = 0

    constructor(context: Context?) : super(context) {

    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        val validStartX = (width - iconSize) / 2
        val validStartY = (height - labelHeight - iconSize) / 2
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                if (e.x < validStartX || e.x > validStartX + iconSize) {
                    return false
                }
                if (e.y < validStartY || e.y > validStartY + iconSize + labelHeight) {
                    return false
                }
            }
        }
        return super.dispatchTouchEvent(e)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var w = iconSize
        val h: Float = iconSize + if (showLabel) labelHeight else 0f
        if (targetedWidth != 0) {
            w = targetedWidth.toFloat()
        }
        setMeasuredDimension(
            ceil(w.toDouble()).toInt(), ceil(h.toDouble()).toInt() + resources.getDimension(R.dimen._2dp).toInt() + targetedHeightPadding * 2
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawIconTop = (height - iconSize - if (showLabel) labelHeight else 0f) / 2f

        if (label != null && showLabel) {
            textPaint.getTextBounds(label, 0, label!!.length, textContainer)
            val maxLabelWidth = width - minIconTextMargin * 2

            if (textContainer.width() > maxLabelWidth) {
                // 处理标签名过长
                val shortLabel = "${label?.substring(0.until(label!!.length - 2))}..."
                textPaint.getTextBounds(shortLabel, 0, shortLabel.length, textContainer)
                canvas.drawText(shortLabel, (width - textContainer.width().toFloat()) / 2, height - drawIconTop, textPaint)
            } else {
                // 绘制文本
                canvas.drawText(label!!, (width - textContainer.width().toFloat()) / 2, height - drawIconTop, textPaint)
            }
        }

        if (icon != null) {
            canvas.save()
            // 偏移画布的原点(0, 0)
            canvas.translate((width - iconSize) / 2, drawIconTop)
            icon!!.setBounds(0, 0, iconSize.toInt(), iconSize.toInt())
            icon!!.draw(canvas)
            if (notificationCount > 0) {
                // TODO 绘制消息计数
            }
            canvas.restore()
        }
    }

    class Builder(private val context: Context?) {
        private var view: AppView = AppView(context)

        fun setIcon(drawable: Drawable?) : Builder {
            view.icon = drawable
            return this
        }

        fun showLabel(isShow: Boolean) : Builder {
            view.showLabel = isShow
            return this
        }

        fun setTargetWidth(w: Int) : Builder {
            view.targetedWidth = w
            return this
        }

        fun setIconSize(size: Float) : Builder {
            view.iconSize = size
            return this
        }

        fun setLabel(label: String?) : Builder {
            view.label = label
            return this
        }

        fun setAppItem(item: DesktopItem): Builder {
            view.label = item.label
            view.icon = item.icon
            view.setOnClickListener {
                Tool.createScaleInScaleOutAnim(view) {
                    //                        Tool.startApp(_view.getContext(), AppManager.getInstance(_view.getContext()).findApp(item._intent), _view);
                }
                try {
                    item.startArgs?.forEach { (key, value) ->
                        item.intent?.putExtra(key, value)
                    }
                    view.context.startActivity(item.intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    //                        ToastUtil.showToast(view.context, "启动失败")
                }
            }
            return this
        }

        fun setGroupItem(
            callback: DesktopCallback?,
            item: DesktopItem
        ): Builder {
            view.label = item.label
            view.icon = GroupDrawable(context, item, context!!.resources.getDimension(R.dimen.desktop_app_icon_size).toInt())
            view.setOnClickListener { v ->
                if (Launcher.instance().groupPopup?.showPopup(item, v, callback) == true) {
                    ((v as AppView).icon as GroupDrawable?)!!.popUp()
                }
            }
            return this
        }

        fun withOnLongClick(item: DesktopItem, desktopCallback: DesktopCallback?): Builder {
            view.setOnLongClickListener(
                DragHandler.longClick(item, desktopCallback)
            )
            return this
        }

        fun build() : AppView {
            return view
        }
    }

}