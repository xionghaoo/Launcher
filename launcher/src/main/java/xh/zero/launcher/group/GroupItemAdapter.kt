package xh.zero.launcher.group

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import xh.zero.launcher.CellContainer
import xh.zero.launcher.repo.DesktopItem

class GroupItemAdapter(
    private val context: Context,
    private val pageNum: Int,
    private val items: HashMap<Int, ArrayList<DesktopItem>>,
    private val initialCell: (CellContainer, List<DesktopItem>) -> Unit
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val cellContainer = CellContainer(context)

        cellContainer.setGridSize(x = 3, y = 3)
        initialCell(cellContainer, items[position + 1] ?: emptyList())
//        Log.d(TAG, "instantiateItem: ${cellContainer.childCount}")
        container.addView(cellContainer)
        cellContainer.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        cellContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        return cellContainer
    }

    override fun getCount(): Int = pageNum

    override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }

    companion object {
        const val TAG = "GroupItemAdapter"
    }
}