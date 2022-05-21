package com.example.skucise.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.example.skucise.Meeting
import com.example.skucise.R
import com.example.skucise.ReqSender
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.adapter.MeetingAdapter
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.calendar_day_layout.view.*
import kotlinx.android.synthetic.main.calendar_event_item_view.view.*
import kotlinx.android.synthetic.main.calendar_month_layout.view.*
import kotlinx.android.synthetic.main.fragment_calendar.*
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [CalendarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

data class Event(val username: String, val title: String, val time: LocalDateTime)

class EventsAdapter : RecyclerView.Adapter<EventsAdapter.EventsViewHolder>() {

    val events = mutableListOf<Event>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsViewHolder {
        return EventsViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.calendar_event_item_view,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: EventsViewHolder, position: Int) {
        viewHolder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventsViewHolder(private val binding: View) : RecyclerView.ViewHolder(binding) {
        fun bind(event: Event) {
            binding.tv_event_username.text = event.username
            binding.tv_event_title.text = event.title
            binding.tv_event_time.text = event.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        }
    }
}

class CalendarFragment : Fragment() {

    // Calendar
    private val today = LocalDate.now()
    private var selectedDate: LocalDate? = null
    private val events = mutableMapOf<LocalDate, ArrayList<Event>>()
    private val eventsAdapter = EventsAdapter()

    // Meeting Requests
    private var meetingRequests: ArrayList<Meeting> = arrayListOf()
    private val meetingRequestAdapter: MeetingAdapter = MeetingAdapter(meetingRequests)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        meetingRequestAdapter.setupNavMenu(requireActivity().nav_bottom_navigator)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize recycler views
        rcv_events.apply {
            adapter = eventsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        }
        rcv_calendar_page.apply {
            adapter = meetingRequestAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Calendar views
        val daysOfWeek = daysOfWeekFromLocale()
        val currentMonth = YearMonth.now()
        calv_calendar_page.apply {
            setup(currentMonth.minusMonths(1), currentMonth.plusMonths(12), daysOfWeek.first())
            scrollToMonth(currentMonth)
        }
        // // for day
        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay // Will be set when this container is bound.
            val binding = view

            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) selectDate(day.date)
                }
            }
        }
        calv_calendar_page.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val textView = container.binding.tv_calender_day_text
                val dotView = container.binding.tv_calender_dot_view

                textView.text = day.date.dayOfMonth.toString()

                textView.background = null
                if (day.owner == DayOwner.THIS_MONTH) {
                    textView.setTextColor(resources.getColor(R.color.black))
                    dotView.isVisible = events[day.date].orEmpty().isNotEmpty()
                    if (dotView.isVisible && day.date < LocalDate.now())
                        dotView.setBackgroundColor(resources.getColor(R.color.red))
                    when (day.date) {
                        today -> {
                            textView.setBackgroundResource(R.drawable.bg_calender_today)
                        }
                        selectedDate -> {
                            textView.setBackgroundResource(R.drawable.bg_calendar_selected)
                        }
                    }
                } else {
                    textView.setTextColor(resources.getColor(R.color.transparent_dark))
                    dotView.visibility = View.INVISIBLE
                }
            }
        }
        // // for month
        calv_calendar_page.monthScrollListener = {
            // Select the first day of the month when
            // we scroll to a new month.
            selectDate(it.yearMonth.atDay(1))
        }
        class MonthViewContainer(view: View) : ViewContainer(view) {
            val textView = view.tv_month
            val legendLayout = view.legendLayout
        }
        calv_calendar_page.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                val date = LocalDate.of(month.year, month.month, 1)
                container.textView.text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                // Setup each header day text if we have not done that already.
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    container.legendLayout.children.map { it as TextView }.forEachIndexed { index, tv ->
                        tv.text = daysOfWeek[index].name.first().toString()
                        tv.setTextColor(resources.getColor(R.color.black))
                    }
                }
            }
        }

        // request meetings
        ReqSender.sendRequestArray(
            requireContext(),
            Request.Method.POST,
            "meeting/get_my_meetings",
            null,
            { response ->
                meetingRequests = ArrayList()
                for (i in 0 until response.length()) {
                    val meeting = response[i] as JSONObject
                    val meetingRequest = Meeting(
                        id            = meeting.getJSONObject("meetingData").getInt("id"),
                        advertId      = meeting.getJSONObject("meetingData").getInt("advertId"),
                        agreedVisitor = meeting.getJSONObject("meetingData").getBoolean("agreedVisitor"),
                        agreedOwner   = meeting.getJSONObject("meetingData").getBoolean("agreedOwner"),
                        concluded     = meeting.getJSONObject("meetingData").getBoolean("concluded"),
                        otherUser     = meeting.getInt("otherUserId"),
                        username      = meeting.getString("otherUsername"),
                        title         = meeting.getString("advertTitle"),
                        owner         = meeting.getBoolean("amIOwner"),
                        proposedTime  = LocalDateTime.parse(
                            meeting.getJSONObject("meetingData").getString("time")),
                        dateCreated   = LocalDateTime.parse(
                            meeting.getJSONObject("meetingData").getString("dateCreated"))
                    )
                    meetingRequests.add(meetingRequest)

                    val eventDate = meetingRequest.proposedTime.toLocalDate()
                    if (events[eventDate] == null) events[eventDate] = ArrayList()
                    events[eventDate]?.add(Event(
                        username = meetingRequest.username,
                        title = meetingRequest.title,
                        time = meetingRequest.proposedTime
                    ))
                }
                meetingRequestAdapter.updateMeetings(meetingRequests)
                if (selectedDate != null) updateAdapterForDate(selectedDate!!)
                calv_calendar_page.notifyCalendarChanged()
            },
            { error ->
                Toast.makeText(context, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { calv_calendar_page.notifyDateChanged(it) }
            calv_calendar_page.notifyDateChanged(date)
            updateAdapterForDate(date)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterForDate(date: LocalDate) {
        eventsAdapter.apply {
            events.clear()
            tv_events.visibility = View.INVISIBLE
            events.addAll(this@CalendarFragment.events[date].orEmpty())
            if(events.size > 0)
                tv_events.visibility = View.VISIBLE
            notifyDataSetChanged()
        }
    }

    private fun daysOfWeekFromLocale(): Array<DayOfWeek> {
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        var daysOfWeek = DayOfWeek.values()
        // Order `daysOfWeek` array so that firstDayOfWeek is at index 0.
        // Only necessary if firstDayOfWeek != DayOfWeek.MONDAY which has ordinal 0.
        if (firstDayOfWeek != DayOfWeek.MONDAY) {
            val rhs = daysOfWeek.sliceArray(firstDayOfWeek.ordinal..daysOfWeek.indices.last)
            val lhs = daysOfWeek.sliceArray(0 until firstDayOfWeek.ordinal)
            daysOfWeek = rhs + lhs
        }
        return daysOfWeek
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment CalendarFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            CalendarFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}