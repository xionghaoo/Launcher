package xh.zero.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class PlainListAdapter<T>(private var _items: List<T>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ItemViewHolder(v: View) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ItemViewHolder(inflater.inflate(itemLayoutId(), parent, false))
    }

    override fun getItemCount(): Int = _items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = _items[position]
        val v = holder.itemView
        bindView(v, item, position)
    }

    open fun updateData(data: List<T>) {
        _items = data
        notifyDataSetChanged()
    }

    abstract fun itemLayoutId(): Int

    abstract fun bindView(v: View, item: T, position: Int)
}