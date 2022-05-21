package com.example.skucise.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skucise.DropdownOption
import com.example.skucise.R
import kotlinx.android.synthetic.main.activity_navigation.view.*
import kotlinx.android.synthetic.main.item_account_dropdown.view.*

class AccountDropdownAdapter(private val options: List<DropdownOption>)
    : RecyclerView.Adapter<AccountDropdownAdapter.AccountDropdownViewHolder>() {

    class AccountDropdownViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountDropdownViewHolder {
        return AccountDropdownViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_account_dropdown,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AccountDropdownViewHolder, position: Int) {
        val currentOption = options[position]
        holder.itemView.apply {
            btn_account_dd_option.text = currentOption.name
            btn_account_dd_option.setTextColor(getResources().getColor(R.color.white))
            btn_account_dd_option.setOnClickListener(currentOption.action)
        }
    }

    override fun getItemCount(): Int {
        return options.size
    }
}