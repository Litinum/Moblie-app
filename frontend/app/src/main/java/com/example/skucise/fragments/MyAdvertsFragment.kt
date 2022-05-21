package com.example.skucise.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.example.skucise.R
import com.example.skucise.ReqSender
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.adapter.AdvertAdapter
import com.example.skucise.loadAdverts
import kotlinx.android.synthetic.main.fragment_my_adverts.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MyAdvertsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyAdvertsFragment : Fragment() {

    private val advertAdapter: AdvertAdapter = AdvertAdapter(allFavorites = false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_adverts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rcv_my_adverts.apply {
            advertAdapter.setupNavMenu(requireActivity().findViewById(R.id.nav_bottom_navigator))
            adapter = advertAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        ReqSender.sendRequestArray(
            this.requireActivity(),
            Request.Method.POST,
            "advert/get_my_adverts",
            null,
            { response ->
                val myAdverts = loadAdverts(response)
                advertAdapter.updateAdverts(myAdverts)
                if(myAdverts.size != 0)
                    tv_my_adverts_none.visibility = View.INVISIBLE
            },
            { error ->
                Toast.makeText(activity, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
            }
        )
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment FavoritesFragment.
         */
        fun newInstance() =
            MyAdvertsFragment().apply {
                arguments = Bundle().apply {}
            }
    }
}