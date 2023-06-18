package com.example.foodmap.model


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.foodmap.R
import com.example.foodmap.data.User

class UserListAdapter(context: Context, private val itemList: ArrayList<User>) : ArrayAdapter<User>(context, 0, itemList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.useradpter_layout, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val item = itemList[position]



        holder.itemUserName.text = item.username
        holder.itemFullName.text = item.firstName
        holder.itemSurName.text = item.lastName
        holder.itemaddCount.text = item.addCount.toString()
        holder.itemstarsCount.text = item.startCount.toString()
        holder.itemcommentsCount.text = item.commentsCount.toString()



        return view!!
    }

    private class ViewHolder(view: View) {
        val itemUserName : TextView = view.findViewById(R.id.tvUserUserName)
        val itemFullName : TextView = view.findViewById(R.id.tvUserName)
        val itemSurName : TextView = view.findViewById(R.id.tvUserSurname)
        val itemaddCount : TextView = view.findViewById(R.id.tvAddCount)
        val itemstarsCount : TextView = view.findViewById(R.id.tvStarsCount)
        val itemcommentsCount : TextView = view.findViewById(R.id.tvCommentsCountr)


    }
}
