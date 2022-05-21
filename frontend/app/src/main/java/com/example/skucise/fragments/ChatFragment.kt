package com.example.skucise.fragments

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.example.skucise.*
import com.example.skucise.MessageJSON.Companion.toRecentMessage
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.adapter.RecentMessageAdapter
import kotlinx.android.synthetic.main.fragment_chat.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [ChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatFragment : Fragment() {
    private val recentMessages: ArrayList<RecentMessage> = ArrayList();
    private val rmAdapter: RecentMessageAdapter = RecentMessageAdapter(recentMessages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rcv_my_recent_messages.apply {
            rmAdapter.setupNavMenu(requireActivity().findViewById(R.id.nav_bottom_navigator))
            adapter = rmAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        ReqSender.sendRequestArray(
            this.requireActivity(),
            Request.Method.POST,
            "message/get_chats",
            null,
            { response ->
                val js = (response as JSONArray)
                recentMessages.clear()
                for(i in 0 until js.length()){
                    val jsonObj = js[i] as JSONObject
                    recentMessages.add(toRecentMessage(jsonObj))
                }
                rmAdapter.updateData()
            },
            { error ->
                Toast.makeText(activity, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
            }
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}