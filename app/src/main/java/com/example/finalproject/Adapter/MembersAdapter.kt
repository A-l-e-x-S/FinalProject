package com.example.finalproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import android.view.View
import com.example.finalproject.Model.Member

class MembersAdapter(private val members: List<Member>) :
    RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.memberName)
        val emailText: TextView = itemView.findViewById(R.id.memberEmail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val (name, email) = members[position]
        holder.nameText.text = name
        holder.emailText.text = email
    }

    override fun getItemCount(): Int = members.size
}
