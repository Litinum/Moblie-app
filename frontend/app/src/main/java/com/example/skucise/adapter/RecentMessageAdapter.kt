package com.example.skucise.adapter

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skucise.*
import com.example.skucise.SessionManager.Companion.BASE_API_URL
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.item_recent_message.*
import kotlinx.android.synthetic.main.item_recent_message.view.*

class RecentMessageAdapter (
    private val recentMessages: ArrayList<RecentMessage> = ArrayList()
) : RecyclerView.Adapter<RecentMessageAdapter.RecentMessageViewHolder>() {

    inner class RecentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentMessageAdapter.RecentMessageViewHolder {
        this.parentFragment = parent
        return RecentMessageViewHolder(LayoutInflater.from(parent.context).inflate(
                R.layout.item_recent_message,
                parent,
                false
            )
        )
    }

    private var navigationView: BottomNavigationView? = null

    fun setupNavMenu(bottomNavigationView: BottomNavigationView) {
        this.navigationView = bottomNavigationView
    }

    fun updateData(){
        notifyDataSetChanged()
    }

    private lateinit var parentFragment : ViewGroup

    fun meetingToMessage(meet: Meeting): Message {
        val time = DateTimeHelper.getReadableDateFull(meet.proposedTime)
        val seen = (meet.owner && meet.agreedOwner) || (!meet.owner && meet.agreedVisitor)
        return Message(
            Id = 0.toUInt(),
            SenderId = (if (!meet.owner && meet.agreedVisitor) meet.otherUser else 0).toUInt(),
            ReceiverId = 0.toUInt(),
            Content = "Sastanak - $time",
            SendDate = meet.dateCreated,
            Seen = seen
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: RecentMessageViewHolder, position: Int) {
        val rm = recentMessages[position]

        holder.itemView.apply {
            var msg: Message
            if(rm.MessageOrMeeting.IsMessage){
                tv_message.setTypeface(null, Typeface.NORMAL)
                msg = rm.MessageOrMeeting.Message!!
            }else{
                tv_message.setTypeface(null, Typeface.ITALIC)
                msg = meetingToMessage(rm.MessageOrMeeting.Meeting!!)
            }

            tv_from.text = rm.User.DisplayName
            tv_message.text = msg.Content
            tv_date.text = DateTimeHelper.getReadableDate(msg.SendDate)
            if(rm.User.Id != msg.ReceiverId) {
                if(msg.Seen) {
                    img_seen.visibility = View.GONE
                }else{
                    img_seen.setBackgroundResource(R.drawable.ic_alert)
                }
            }else{
                if(msg.Seen) {
                    img_seen.setBackgroundResource(R.drawable.ic_message_seen)
                }else{
                    img_seen.setBackgroundResource(R.drawable.ic_message_sent)
                }
            }

            img_user.clipToOutline = true
            Glide.with(context)
                .load("${BASE_API_URL}image/get_user_image_file?userId=${rm.User.Id}")
                    .centerCrop()
                .placeholder(R.drawable.ic_offline)
                .into(img_user)

            img_status.clipToOutline = true
            var statusIcon = R.drawable.ic_offline
            var statusColor = R.color.msgs_unavailable
            if(rm.User.Online) {
                statusIcon = R.drawable.ic_online
                statusColor = R.color.msgs_available
            }
            img_status.foreground =  context.getResources().getDrawable(statusIcon)
            img_status.foregroundTintList = context.getResources().getColorStateList(statusColor)

            csl_recent_message.setOnClickListener {
                if (navigationView == null) return@setOnClickListener

                navigationView!!.menu.setGroupCheckable(0, true, false)
                for (i in 0 until navigationView!!.menu.size()) {
                    navigationView!!.menu.getItem(i).isChecked = false
                }
                navigationView!!.menu.setGroupCheckable(0, true, true)

                val args = Bundle()
                args.putInt("otherUserId", rm.User.Id.toInt())
                parentFragment.findNavController().navigate(R.id.chatWithUserFragment, args)
            }
        }
    }

    override fun getItemCount(): Int {
        return recentMessages.size
    }
}