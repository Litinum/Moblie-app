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
import kotlinx.android.synthetic.main.fragment_favorites.*

/**
 * A simple [Fragment] subclass.
 * Use the [FavoritesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FavoritesFragment : Fragment() {

    private val advertAdapter: AdvertAdapter = AdvertAdapter(allFavorites = true)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rcv_favorite_adverts.apply {
            advertAdapter.setupNavMenu(requireActivity().findViewById(R.id.nav_bottom_navigator))
            adapter = advertAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        ReqSender.sendRequestArray(
            this.requireActivity(),
            Request.Method.POST,
            "advert/get_favourite_adverts",
            null,
            { response ->
                val favoriteAdverts = loadAdverts(response)
                advertAdapter.updateAdverts(favoriteAdverts)
                if(favoriteAdverts.size != 0)
                    tv_favourites_none.visibility = View.INVISIBLE
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
            FavoritesFragment().apply {
                arguments = Bundle().apply {}
            }
    }
}