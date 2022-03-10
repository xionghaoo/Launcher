package xh.zero.launcher.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import xh.zero.launcher.ItemOptionView
import xh.zero.launcher.R

class LauncherRootView : ItemOptionView {

    constructor(context: Context) : super(context) {

    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflate(context, R.layout.launcher_root_view, this)
    }
}