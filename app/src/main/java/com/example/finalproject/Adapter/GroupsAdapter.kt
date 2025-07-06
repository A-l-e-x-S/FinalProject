package com.example.finalproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import com.example.finalproject.Model.GroupEntity
import com.squareup.picasso.Picasso

class GroupsAdapter(
    private val groups: List<GroupEntity>,
    private val onGroupClick: (GroupEntity) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupImageView: ImageView = itemView.findViewById(R.id.groupImageView)
        val groupNameTextView: TextView = itemView.findViewById(R.id.groupNameTextView)
        val groupTypeTextView: TextView = itemView.findViewById(R.id.groupTypeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.groupNameTextView.text = group.groupName
        holder.groupTypeTextView.text = group.groupType

        Picasso.get()
            .load(group.groupPhotoUrl)
            .placeholder(R.drawable.ic_group_placeholder)
            .into(holder.groupImageView)

        holder.itemView.setOnClickListener { onGroupClick(group) }
    }

    override fun getItemCount(): Int = groups.size
}