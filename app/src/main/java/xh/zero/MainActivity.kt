package xh.zero

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import xh.zero.launcher.Launcher
import xh.zero.launcher.SystemUtil

class MainActivity : AppCompatActivity() {

    private val launcher = Launcher.instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        setContentView(R.layout.activity_main)

        launcher.initial(this, findViewById(R.id.root_view), listOf())
    }

    override fun onDestroy() {
        launcher.destroy()
        super.onDestroy()
    }
}