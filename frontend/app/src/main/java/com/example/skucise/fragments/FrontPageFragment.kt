package com.example.skucise.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.example.skucise.FilterArray
import com.example.skucise.R
import com.example.skucise.ReqSender
import com.example.skucise.adapter.AdvertAdapter
import com.example.skucise.adapter.CityTilesAdapter
import com.example.skucise.TileSet
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.loadAdverts
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.fragment_frontpage.*
import kotlinx.android.synthetic.main.fragment_frontpage.view.*
import kotlin.math.min

class FrontPageFragment : Fragment(R.layout.fragment_frontpage) {

    private val advertAdapter: AdvertAdapter = AdvertAdapter(type = 1)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_frontpage, container, false)

        // Load cities from server once
        if (SearchFragment.allCities == null) {
            ReqSender.sendRequestArray(
                requireActivity(),
                Request.Method.GET,
                "advert/get_all_cities",
                null,
                { cities ->
                    val cityArray = Array(
                        cities.length()
                    ) { i -> cities[i].toString() }
                    loadCities(cityArray, view)
                    SearchFragment.allCities = cityArray
                },
                { error ->
                    Toast.makeText(activity, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                }
            )
        }
        else {
            loadCities(SearchFragment.allCities!!, view)
        }

        // Load new adverts
        ReqSender.sendRequestArray(
            requireActivity(),
            Request.Method.POST,
            "advert/get_recent_adverts",
            hashMapOf(Pair("numOfAdverts", "10")),
            { results ->
                val adverts = loadAdverts(results)
                advertAdapter.updateAdverts(adverts)
            },
            { error ->
                Toast.makeText(activity, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
            }
        )

        // Setup new adverts recycler view
        view.rcv_new_adverts.apply {
            advertAdapter.setupNavMenu(requireActivity().findViewById(R.id.nav_bottom_navigator))
            adapter = advertAdapter
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_search_sell_options.setOnClickListener {
            findNavController().navigate(R.id.addAdvertFragment)
        }

        btn_search_buy_options.setOnClickListener {
            val filters = FilterArray()
            filters.applyFilter(FilterArray.FilterNames.SaleType, FilterArray.SaleTypes.Purchase.ordinal)

            val args = Bundle()
            args.putString("advertsFilterArray", filters.getFilters())
            findNavController().navigate(requireActivity().nav_bottom_navigator.menu[1].itemId, args)
        }

        btn_search_rent_options.setOnClickListener {
            val filters = FilterArray()
            filters.applyFilter(FilterArray.FilterNames.SaleType, FilterArray.SaleTypes.Rent.ordinal)

            val args = Bundle()
            args.putString("advertsFilterArray", filters.getFilters())
            findNavController().navigate(requireActivity().nav_bottom_navigator.menu[1].itemId, args)
        }

    }

    private fun loadCities(cities: Array<String>, view: View) {
        /*var s = ""
                for (i in 0 until cities.length()){
                    println()
                    s += "\"" + cities[i].toString() + "\" to R.drawable." + cities[i].toString().lowercase().replace(' ', '_') + ",\n"
                }
                Log.i("tag", s)*/
        val tileSet = mutableListOf<TileSet>()
        for (i in 0 until min(cities.size / 3, 5)) {
            tileSet.add(
                TileSet(
                    cities[3 * i],
                    3 * i,
                    cities[3 * i + 1],
                    3 * i + 1,
                    cities[3 * i + 2],
                    3 * i + 2
                )
            )
        }
        //Toast.makeText(context, "test: mounted = " + Environment.getExternalStorageState(), Toast.LENGTH_LONG).show()
        val cityTilesAdapter = CityTilesAdapter(tileSet, requireActivity().nav_bottom_navigator)
        val a = view.findViewById<RecyclerView>(R.id.rcv_city_tiles)
        a.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        a.adapter = cityTilesAdapter
    }
}