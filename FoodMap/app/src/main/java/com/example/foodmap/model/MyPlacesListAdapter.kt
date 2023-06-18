package com.example.foodmap.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.foodmap.R
import com.example.foodmap.data.MyPlace

class MyPlacesListAdapter(context: Context, private val itemList: ArrayList<MyPlace>) : ArrayAdapter<MyPlace>(context, 0, itemList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.tabela_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val item = itemList[position]



        holder.itemTitle.text = item.name
        holder.itemAutor.text = item.autor
        holder.itemTip.text = item.tip

        var sum:Double = 0.0
        for (el in item.grades!!)
            sum += el.value
        if (item.grades.size!=0)
            sum/=item.grades.size

        holder.itemOcena.text = sum.toString()

        return view!!
    }

    private class ViewHolder(view: View) {
        val itemTitle: TextView = view.findViewById(R.id.elIme)
        val itemAutor : TextView =view.findViewById(R.id.elAutor)
        val itemOcena : TextView = view.findViewById(R.id.elOcena)
        val itemTip : TextView = view.findViewById(R.id.elTip)

    }
}
