package xh.zero.launcher.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import xh.zero.launcher.R

class DeleteOptionView : FrameLayout {

    constructor(context: Context) : super(context) {

    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.overlay_delete))

        isFocusable = true
        isClickable = true

        val optionView = LayoutInflater.from(context).inflate(R.layout.launcher_delete_option_view, null)
        addView(optionView)
        val lp = optionView.layoutParams as FrameLayout.LayoutParams
        lp.gravity = Gravity.CENTER
        lp.width = LayoutParams.WRAP_CONTENT
        lp.height = LayoutParams.WRAP_CONTENT
    }

    fun setOnCancelListener(listener: () -> Unit) {
        findViewById<View>(R.id.launcher_btn_cancel).setOnClickListener {
            listener()
        }
    }

    fun setOnConfirmListener(listener: () -> Unit) {
        findViewById<View>(R.id.launcher_btn_confirm).setOnClickListener {
            listener()
        }
    }

    fun setAppInfo(name: String?, icon: Drawable?) {
        findViewById<ImageView>(R.id.launcher_iv_app_icon).setImageDrawable(icon)
        findViewById<TextView>(R.id.launcher_btn_name).text = name
    }
}