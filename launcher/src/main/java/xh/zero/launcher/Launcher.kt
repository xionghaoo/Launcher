package xh.zero.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import xh.zero.launcher.group.GroupPopupView
import xh.zero.launcher.repo.LauncherRepository

class Launcher {
    companion object {

        private var INSTANCE: Launcher? = null

        fun instance() : Launcher {
            if (INSTANCE == null) {
                INSTANCE = Launcher()
            }
            return INSTANCE!!
        }

    }

    var itemOptionView: ItemOptionView? = null
        private set

    var desktop: Desktop? = null
        private set

    var groupPopup: GroupPopupView? = null
        private set

    var repo: LauncherRepository? = null
        private set

    private var context: Activity? = null
    private val dragHandler = DragHandler()

    fun initial(activity: AppCompatActivity, rootView: ItemOptionView, homePages: List<Fragment>) {
        context = activity

        this.itemOptionView = rootView
        this.groupPopup = activity.findViewById(R.id.groupPopup)
        this.desktop = activity.findViewById(R.id.desktop)
        this.repo = desktop?.repo

        dragHandler.initDragNDrop(
            itemOptionView!!,
            activity.findViewById(R.id.leftDragHandle),
            activity.findViewById(R.id.rightDragHandle),
            activity.findViewById(R.id.topDragHandle),
            desktop!!,
            activity.findViewById(R.id.deleteOptionView)
        )

        desktop!!.initial(activity.supportFragmentManager, homePages)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_INSTALL)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addDataScheme("package")
        activity.registerReceiver(receiver, intentFilter)
    }

    fun destroy() {
        context?.unregisterReceiver(receiver)
        itemOptionView = null
        desktop = null
        groupPopup = null
        repo = null
    }


    // app 安装卸载监听
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
//            Timber.d("on receiver: ${intent?.action}")
            val packageName: String? = intent?.data?.encodedSchemeSpecificPart
            when(intent?.action) {
                Intent.ACTION_PACKAGE_INSTALL -> {

                }
                Intent.ACTION_PACKAGE_ADDED -> {
                    // 应用安装
//                    binding.desktop.onAppInstall(viewModel.repo, packageName)
                    desktop?.onAppInstall(packageName)
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    // 应用卸载
//                    binding.desktop.onAppUninstall(viewModel.repo, packageName)
                    desktop?.onAppUninstall(packageName)
                }
                Intent.ACTION_PACKAGE_CHANGED -> {
                    // 应用更新
                }
            }
        }
    }


}