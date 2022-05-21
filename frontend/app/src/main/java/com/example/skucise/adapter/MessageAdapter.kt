package com.example.skucise.adapter

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skucise.*
import com.example.skucise.DateTimeHelper.Companion.getDateAgeClass
import com.example.skucise.DateTimeHelper.Companion.sameAgeClass
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.item_meeting_request.view.*
import kotlinx.android.synthetic.main.item_message.view.*
import kotlinx.android.synthetic.main.item_message.view.img_seen
import kotlinx.android.synthetic.main.item_message.view.img_user
import kotlinx.android.synthetic.main.item_message.view.tv_date
import kotlinx.android.synthetic.main.item_message.view.tv_message
import java.time.Duration
import java.time.format.DateTimeFormatter
import android.text.TextPaint

import android.text.method.LinkMovementMethod

import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import com.example.skucise.DateTimeHelper.Companion.getReadableDateFull
import kotlinx.android.synthetic.main.activity_navigation.view.*


class MessageAdapter (
    private val otherUserId: UInt,
    private val messages: ArrayList<MessageOrMeeting> = ArrayList(),
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageAdapter.MessageViewHolder {
        this.parentFragment = parent
        return MessageViewHolder(LayoutInflater.from(parent.context).inflate(
                R.layout.item_message,
                parent,
                false
            )
        )
    }

    private var navigationView: BottomNavigationView? = null

    fun setupNavMenu(bottomNavigationView: BottomNavigationView) {
        this.navigationView = bottomNavigationView
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateData(){
        notifyDataSetChanged()
    }

    private lateinit var parentFragment : ViewGroup

    var height = 0

    @RequiresApi(Build.VERSION_CODES.O)
    fun meetingToMessage(meet: Meeting): Message {
        val seen = (meet.owner && meet.agreedOwner) || (!meet.owner && meet.agreedVisitor)
        return Message(
            Id = 0.toUInt(),
            SenderId = (if (!meet.owner && meet.agreedVisitor) meet.otherUser else 0).toUInt(),
            ReceiverId = 0.toUInt(),
            Content = "Sastanak",
            SendDate = meet.dateCreated,
            Seen = seen
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msgOrMeeting = messages[position]

        val layouts = arrayListOf(
            R.drawable.rounded_message,
            R.drawable.rounded_message_top,
            R.drawable.rounded_message_bottom,
            R.drawable.rounded_message_middle,
            R.drawable.rounded_message,
            R.drawable.rounded_message_top_rtl,
            R.drawable.rounded_message_bottom_rtl,
            R.drawable.rounded_message_middle_rtl
        )
        var customLayout = 0
        var userOffset = 0
        val minTimeDiff = Duration.ofMinutes(5)

        var hideTime = false

        var msg: Message
        var meet: Meeting
        if(msgOrMeeting.IsMessage){
            msg = msgOrMeeting.Message!!
        }else{
            meet = msgOrMeeting.Meeting!!
            msg = meetingToMessage(meet)
        }

        val ageClass = getDateAgeClass(msg.SendDate)

        if(position > 0) {
            val prevMessageOrMeeting = messages[position - 1]
            var prevMessage: Message
            var prevMeet: Meeting
            if(prevMessageOrMeeting.IsMessage){
                prevMessage = prevMessageOrMeeting.Message!!
            }else{
                prevMeet = prevMessageOrMeeting.Meeting!!
                prevMessage = meetingToMessage(prevMeet)
            }
            val sameAge = sameAgeClass(msg.SendDate, prevMessage.SendDate)
            val sameSender = prevMessage.SenderId == msg.SenderId
            if(sameAge){
                when(ageClass) {
                    0 -> { //Seconds
                        if(Duration.between(prevMessage.SendDate, msg.SendDate) < Duration.ofSeconds(30))
                            hideTime = true
                    }
                    1 -> { //Minutes
                        if(Duration.between(prevMessage.SendDate, msg.SendDate) < Duration.ofMinutes(2))
                            hideTime = true
                    }
                    2 -> { //Hours
                        if(Duration.between(prevMessage.SendDate, msg.SendDate) < Duration.ofHours(1))
                            hideTime = true
                    }
                    3, 4, 5 -> { //DayOfWeek, DayOfMonth, DayOfYear
                        if(Duration.between(prevMessage.SendDate, msg.SendDate) < Duration.ofDays(1))
                            hideTime = true
                    }

                }
                if(sameSender && hideTime)
                    customLayout += 2
            }
        }

        if(position < messages.size - 1){
            val nextMessageOrMeeting = messages[position + 1]
            var nextMessage: Message
            var nextMeet: Meeting
            if(nextMessageOrMeeting.IsMessage){
                nextMessage = nextMessageOrMeeting.Message!!
            }else{
                nextMeet = nextMessageOrMeeting.Meeting!!
                nextMessage = meetingToMessage(nextMeet)
            }
            val sameAge = sameAgeClass(msg.SendDate, nextMessage.SendDate)
            val sameSender = nextMessage.SenderId == msg.SenderId
            if(sameAge && sameSender)
                customLayout += 1
        }

        if(msg.SenderId != otherUserId)
            userOffset = 4

        holder.itemView.apply {
            if(hideTime) {
                tv_date.visibility = View.GONE
            }
            else {
                tv_date.visibility = View.VISIBLE
            }
            if(height==0)
                this@MessageAdapter.height = csl_user_image_holder.layoutParams.height
            if(customLayout == 0 || customLayout == 2){
                img_user.clipToOutline = true
                Glide.with(context)
                    .load("${SessionManager.BASE_API_URL}image/get_user_image_file?userId=${otherUserId}")
                    .centerCrop()
                    .placeholder(R.drawable.ic_offline)
                    .into(img_user)
                csl_user_image_holder.visibility = View.VISIBLE
                csl_user_image_holder.layoutParams.height = this@MessageAdapter.height

                // Link to user
                csl_user_image_holder.setOnClickListener {
                    if (navigationView == null) return@setOnClickListener

                    navigationView!!.menu.setGroupCheckable(0, true, false)
                    for (i in 0 until navigationView!!.menu.size()) {
                        navigationView!!.menu.getItem(i).isChecked = false
                    }
                    navigationView!!.menu.setGroupCheckable(0, true, true)

                    val args = Bundle()
                    args.putInt("userId", otherUserId.toInt())
                    parentFragment.findNavController().navigate(R.id.myAccountFragment, args)
                }
            }else{
                csl_user_image_holder.visibility = View.INVISIBLE
                csl_user_image_holder.layoutParams.height = 1
            }
            csl_message_content.background = resources.getDrawable(layouts[customLayout+userOffset])

            var readableDate = DateTimeHelper.getReadableDate(msg.SendDate)
            if(ageClass == -1) readableDate = "Upravo poslato"
            tv_date.text = readableDate
            if(otherUserId == msg.SenderId) {
                img_seen.visibility = View.GONE
                ll_message.layoutDirection = View.LAYOUT_DIRECTION_LTR
                csl_message_content.backgroundTintList = context.getResources().getColorStateList(R.color.msgs_background)
                img_seen.foregroundTintList = context.getResources().getColorStateList(R.color.msgs_grey)
                tv_message.setTextColor(resources.getColor(R.color.black))
            }else{
                if(msg.Seen) {
                    img_seen.visibility = View.VISIBLE
                    img_seen.setBackgroundResource(R.drawable.ic_message_seen)
                }else{
                    img_seen.visibility = View.GONE
                }
                csl_user_image_holder.visibility = View.GONE
                ll_message.layoutDirection = View.LAYOUT_DIRECTION_RTL
                csl_message_content.backgroundTintList = context.getResources().getColorStateList(R.color.logo_color)
                tv_message.setTextColor(resources.getColor(R.color.white))
            }

            if(msgOrMeeting.IsMessage){
                tv_message.text = msg.Content
            }else{
                val a = "Sastanak zakazan za "
                val b = "oglas"
                val c = ", u "
                val d = getReadableDateFull(msgOrMeeting.Meeting!!.proposedTime)
                val res = "$a$b$c$d"
                val sb = SpannableString( res );
                val clickableSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(textView: View) {
                        if(navigationView == null) return

                        navigationView!!.menu.setGroupCheckable(0, true, false)
                        for (i in 0 until navigationView!!.menu.size()) {
                            navigationView!!.menu.getItem(i).isChecked = false
                        }
                        navigationView!!.menu.setGroupCheckable(0, true, true)

                        val args = Bundle()
                        args.putInt("advertId", msgOrMeeting.Meeting!!.advertId.toInt())
                        findNavController().navigate(R.id.advertFragment, args)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = context.getColor(R.color.msgs_link)
                        ds.isUnderlineText = true
                    }
                }

                sb.setSpan(StyleSpan(Typeface.BOLD), res.indexOf(d), res.indexOf(d)+d.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                sb.setSpan(ForegroundColorSpan(context.getColor(R.color.msgs_meeting)), 0, res.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(clickableSpan, res.indexOf(b), res.indexOf(b)+b.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //link

                csl_message_content.backgroundTintList = context.getResources().getColorStateList(R.color.msgs_meeting_background)

                tv_message.setMovementMethod(LinkMovementMethod.getInstance())
                tv_message.setText(sb, TextView.BufferType.SPANNABLE)
            }
            /*img_status.clipToOutline = true
            var statusIcon = R.drawable.ic_offline
            var statusColor = R.color.msgs_unavailable
            if(rm.User.Online) {
                statusIcon = R.drawable.ic_online
                statusColor = R.color.msgs_available
            }
            img_status.foreground =  context.getResources().getDrawable(statusIcon)
            img_status.foregroundTintList = context.getResources().getColorStateList(statusColor)
            */
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}