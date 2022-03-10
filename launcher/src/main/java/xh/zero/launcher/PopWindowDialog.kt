package xh.zero.launcher

import android.app.Activity
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import android.widget.PopupWindow

class PopWindowDialog private constructor(
    private val context: Activity,
    private val popupWindow: PopupWindow,
    private val isFade: Boolean
) {
    internal class Builder(private val context: Activity) {

        private var contentView: View? = null
        private var width: Int = context.resources.getDimension(R.dimen._500dp).toInt()
        private var height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
        private var isOutsideTouchable: Boolean = true
        private var topMargin: Int? = null
        private var isShowAnimation: Boolean = true
        private var isFocusable: Boolean = false
        private var isCancellable: Boolean = true
        private var gravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        private var animation: Int = R.style.pop_animation
        private var isFade: Boolean = true

        fun width(w: Int): Builder {
            width = w
            return this
        }

        fun height(h: Int): Builder {
            height = h
            return this
        }

        fun marginTop(top: Int) : Builder {
            topMargin = top
            return this
        }

        fun isShowAnimation(isAnim: Boolean) : Builder {
            isShowAnimation = isAnim
            return this
        }

        fun contentView(v: View): Builder {
            contentView = v
            return this
        }

        /**
         * 移除渐变背景
         */
        fun removeBackground() : Builder {
            this.isFade = false
            return this
        }

        fun isOutsideTouchable(touchable: Boolean) : Builder {
            isOutsideTouchable = touchable
            return this
        }

        /**
         * 默认为false，设置为true会带出导航栏和状态栏，一般在有文本输入框时使用
         */
        fun isFocusable(focusable: Boolean) : Builder {
            isFocusable = focusable
            return this
        }

        fun isCancellable(cancelable: Boolean) : Builder {
            isCancellable = cancelable
            return this
        }

        fun setGravity(_gravity: Int) : Builder {
            gravity = _gravity
            return this
        }

        fun animation(anim: Int) : Builder {
            animation = anim
            return this
        }

        fun build(): PopWindowDialog {
            val popupWindow = PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            val container = LayoutInflater.from(context).inflate(R.layout.launcher_popwindow_dialog_container, null) as FrameLayout
            container.addView(contentView)
            val lp = contentView!!.layoutParams as FrameLayout.LayoutParams
            lp.width = width
            lp.height = height
            if (height != ViewGroup.LayoutParams.MATCH_PARENT) {
                lp.bottomMargin = context.resources.getDimension(R.dimen._32dp).toInt()
            }
            if (topMargin != null) {
                lp.topMargin = topMargin!!
            }
            lp.gravity = gravity
            popupWindow.contentView = container
            if (isOutsideTouchable && isCancellable) {
                contentView?.isFocusable = true
                contentView?.isClickable = true
                container.setOnClickListener {
                    popupWindow.dismiss()
                }
            }
            if (isShowAnimation) {
                popupWindow.animationStyle = animation
            }
            popupWindow.isOutsideTouchable = isOutsideTouchable
//            if (isFade) {
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                    popupWindow.setBackgroundDrawable(BitmapDrawable())
//                }
//            }
//            if (isCancellable) {
//                contentView?.findViewById<View>(R.id.dialog_close)?.setOnClickListener {
//                    popupWindow.dismiss()
//                }
//            } else {
//                contentView?.findViewById<View>(R.id.dialog_close)?.visibility = View.GONE
//            }
            popupWindow.isFocusable = isFocusable
            return PopWindowDialog(context, popupWindow, isFade)
        }
    }

    fun show(v: View? = null, isFullScreen: Boolean = false, isTopLevel: Boolean = false) {
//        val originFocus = popupWindow.isFocusable
//        popupWindow.isFocusable = false
//        popupWindow.update()
//        popupWindow.showAtLocation(v ?: context.window.decorView, gravity, 0, 0)
        if (isFullScreen) {
            SystemUtil.hideSystemUI(popupWindow.contentView)
        }
//        popupWindow.isFocusable = originFocus
//        popupWindow.update()
        if (isTopLevel) {
            popupWindow.windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        popupWindow.showAtLocation(v ?: context.window.decorView, Gravity.CENTER, 0, 0)
        // 弹窗背景变暗
        if (isFade) {
            val container: View = popupWindow.contentView.parent as View
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val p = container.layoutParams as WindowManager.LayoutParams
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            p.dimAmount = 0.3f
            wm.updateViewLayout(container, p)
        }
    }

    fun hideSystemUI() {
        SystemUtil.hideSystemUI(popupWindow.contentView)
        popupWindow.update()
    }

    fun dismiss() {
        popupWindow.dismiss()
    }
}