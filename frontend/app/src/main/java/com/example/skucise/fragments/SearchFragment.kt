package com.example.skucise.fragments

import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.example.skucise.*
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.adapter.AdvertAdapter
import kotlinx.android.synthetic.main.fragment_search.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "advertsFilterArray"

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment() {

    private var adverts: ArrayList<Advert> = ArrayList()
    private var currentPage:    Int = 1
    private var numberOfPages:  Int = 1
    private var advertsPerPage: Int = 10
    private var sortBy:      String = "Sortiraj po"
    private var searchQuery: String = ""

    private var filterViews: HashMap<String, Any>? = null
    private val advertAdapter : AdvertAdapter = AdvertAdapter(adverts)

    private lateinit var r : Resources
    private var px = 0


    private fun initCheckboxMargins(fl: Float) {
        r = requireContext().resources
        px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            fl,
            r.displayMetrics
        ).toInt()
    }

    override fun onResume() {
        // Sort dropdown menu
        val sortOptions = arrayOf(
            "Cena opadajuća",
            "Cena rastuća",
            "Kvadratura opadajuća",
            "Kvadratura rastuća",
            "Staros opadajuća",
            "Staros rastuća"
        )
        val sortArrayAdapter = ArrayAdapter(requireContext(), R.layout.item_sort_by_dropdown, sortOptions)
        atv_sort_by.setAdapter(sortArrayAdapter)

        // Ads per page dropdown menu
        val numberOfAds = arrayOf(
            "10",
            "15",
            "25",
            "50"
        )
        val adNumArrayAdapter = ArrayAdapter(requireContext(), R.layout.item_ads_per_page_dropdown, numberOfAds)
        atv_paging_ads_per_page.setAdapter(adNumArrayAdapter)

        return super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCheckboxMargins(8.0f)

        var filterArray : String? = null

        // load previous state if it exists
        if (fragmentState != null) {
            filterArray    = fragmentState!!["filterArray"]    as String
            currentPage    = fragmentState!!["currentPage"]    as Int
            numberOfPages  = fragmentState!!["numberOfPages"]  as Int
            advertsPerPage = fragmentState!!["advertsPerPage"] as Int
            sortBy         = fragmentState!!["sortBy"]         as String
            searchQuery    = fragmentState!!["searchQuery"]    as String
        } else {
            fragmentState = HashMap()
            fragmentState!!["filterArray"]    = "[]"
            fragmentState!!["currentPage"]    = currentPage
            fragmentState!!["numberOfPages"]  = numberOfPages
            fragmentState!!["advertsPerPage"] = advertsPerPage
            fragmentState!!["sortBy"]         = sortBy
            fragmentState!!["searchQuery"]    = searchQuery
        }

        // if there have been new arguments sent they take priority
        arguments?.let {
            filterArray = it.getString(ARG_PARAM1)
            fragmentState!!["filterArray"] = filterArray!!
        }

        // load state
        if (filterArray != null)
            performAdvertsRequest(filterArray!!)
        else
            performAdvertsRequest("[]")

        // Connecting adverts recycler view
        rcv_search_adverts.apply {
            advertAdapter.setupNavMenu(requireActivity().findViewById(R.id.nav_bottom_navigator))
            adapter = advertAdapter
            layoutManager = LinearLayoutManager(activity)
            isNestedScrollingEnabled = false
        }

        // Construct Filters
        createFilterFields()

        // Filter view toggle
        btn_filters.setOnClickListener {
            csl_filters.visibility = View.VISIBLE
        }
        btn_filters_apply.setOnClickListener {
            csl_filters.visibility = View.GONE
            // Apply filters
            requestAdverts()
        }

        // Preform sorting when necessary
        atv_sort_by.setText(sortBy)
        atv_sort_by.doOnTextChanged { _, _, _, _ ->
            sortBy = atv_sort_by.text.toString()
            performAdvertsRequest(fragmentState!!["filterArray"] as String)
        }

        // change ads per page
        atv_paging_ads_per_page.setText(advertsPerPage.toString())
        atv_paging_ads_per_page.doOnTextChanged { _, _, _, _ ->
            advertsPerPage = atv_paging_ads_per_page.text.toString().toInt()
            performAdvertsRequest(fragmentState!!["filterArray"] as String, true)
        }

        // Paging buttons
        btn_paging_next.setOnClickListener {
            if (currentPage < numberOfPages) {
                currentPage += 1
                performAdvertsRequest(fragmentState!!["filterArray"] as String)
            }
        }
        btn_paging_previous.setOnClickListener {
            if (currentPage > 1) {
                currentPage -= 1
                performAdvertsRequest(fragmentState!!["filterArray"] as String)
            }
        }

        // Dealing with query's
        sv_adverts.setQuery("\u200B", false)
        sv_adverts.setOnQueryTextListener(object: SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText == null || newText.isEmpty())
                    sv_adverts.setQuery("\u200B", false)
                return true
            }

            override fun onQueryTextSubmit(newText: String?): Boolean {
                searchQuery = newText!!.replace("\u200B","")
                performAdvertsRequest(fragmentState!!["filterArray"] as String, true)
                return false
            }
        })
    }

    private fun updatePaging() {
        et_paging_current.setText(currentPage.toString())
        btn_paging_previous.visibility = if (currentPage == 1) View.INVISIBLE else View.VISIBLE
        btn_paging_next.visibility = if (currentPage == numberOfPages) View.INVISIBLE else View.VISIBLE
    }

    private fun createCheckbox(into: ConstraintLayout, name: String, checked: Boolean = false) : CheckBox {
        val checkBox = CheckBox(context)
        checkBox.id = View.generateViewId()
        checkBox.text = name
        checkBox.isChecked = checked
        val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(px, 0, 0, 0)
        checkBox.layoutParams = params
        checkBox.setBackgroundResource(R.drawable.checkbox_selector_shape)
        checkBox.setTextColor(AppCompatResources.getColorStateList(requireContext(), R.color.selector_color))
        //checkBox.setTextColor(R.drawable.checkbox_selector_text_color)
        into.addView(checkBox)
        return checkBox
    }

    private fun listCheckBoxes(into: ConstraintLayout, array: Iterable<Any>) : ArrayList<CheckBox> {
        val checkBoxList = ArrayList<CheckBox>()
        var previous : CheckBox? = null
        for (element in array) {
            val current = createCheckbox(into, element.toString())
            if (previous != null) {
                current.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToEnd = previous!!.id
                }
            }
            checkBoxList.add(current)
            previous = current
        }
        return checkBoxList
    }

    private fun checkBoxesAsArrayList(checkBoxes: ArrayList<CheckBox>): ArrayList<String> {
        val checkedCheckBoxes = ArrayList<String>()
        for (checkBox in checkBoxes) {
            if (checkBox.isChecked) checkedCheckBoxes.add(checkBox.text.toString())
        }
        return checkedCheckBoxes
    }

    private fun createRadioGroup(into: LinearLayout, options: Array<String>, selected: Int = 0): RadioGroup {
        val radioGroup = RadioGroup(context)
        radioGroup.orientation = RadioGroup.HORIZONTAL
        for (option in options) {
            val radioButton = RadioButton(context)
            radioButton.text = option
            radioButton.setTextColor(AppCompatResources.getColorStateList(requireContext(), R.color.selector_color))
            val params = LinearLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(px, 0, 0, 0)
            radioButton.layoutParams = params
            radioButton.setBackgroundResource(R.drawable.checkbox_selector_shape)
            //radioButton.setTextColor(R.drawable.checkbox_selector_text_color)
            radioGroup.addView(radioButton)
        }
        radioGroup.check(selected)
        into.addView(radioGroup)
        return radioGroup
    }

    private fun createFilterFields() {
        // only if needed
        if (filterViews != null) return

        // Residence type
        val checkBoxHouse = createCheckbox(csl_filters_residence_type, "Kuća")
        val checkBoxApartment = createCheckbox(csl_filters_residence_type, "Stan")
        checkBoxApartment.updateLayoutParams<ConstraintLayout.LayoutParams> {
            startToEnd = checkBoxHouse.id
        }

        // Sale type
        val checkBoxBuy = createCheckbox(csl_filters_sale_type, "Prodaja")
        val checkBoxRent = createCheckbox(csl_filters_sale_type, "Iznajmljivanje")
        checkBoxRent.updateLayoutParams<ConstraintLayout.LayoutParams> {
            startToEnd = checkBoxBuy.id
        }

        // Structure type
        val checkBoxStructureType = listCheckBoxes(
            csl_filters_structure_type,
            StructureType.values().asIterable()
        )

        // City
        var checkBoxCity : ArrayList<CheckBox> = ArrayList()
        if (allCities == null) {
            ReqSender.sendRequestArray(
                requireActivity(), Request.Method.GET,
                "advert/get_all_cities", null,
                { cities ->
                    val cityArray = Array(
                        cities.length()
                    ) { i -> cities[i].toString() }
                    checkBoxCity = listCheckBoxes(csl_filters_city, cityArray.asIterable())
                    allCities = cityArray
                },
                { error ->
                    Toast.makeText(activity, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            checkBoxCity = listCheckBoxes(csl_filters_city, allCities!!.asIterable())
        }

        // Number of rooms
        val radioGroupNumOfRooms = createRadioGroup(
            ll_filters_number_of_rooms,
            arrayOf("1+", "2+", "3+", "4+", "5+")
        )

        // Number of bathrooms
        val radioGroupNumOfBathrooms = createRadioGroup(
            ll_filters_number_of_bathrooms,
            arrayOf("1+", "2+", "3+")
        )

        // furnished
        val radioGroupFurnished = createRadioGroup(
            ll_filters_furnished,
            arrayOf("Da", "Ne", "Nebitno")
        )

        filterViews = HashMap()
        filterViews!!["house"]     = checkBoxHouse
        filterViews!!["apartment"] = checkBoxApartment
        filterViews!!["buy"]       = checkBoxBuy
        filterViews!!["rent"]      = checkBoxRent
        filterViews!!["structure"] = checkBoxStructureType
        filterViews!!["city"]      = checkBoxCity
        filterViews!!["bedrooms"]  = radioGroupNumOfRooms
        filterViews!!["bathrooms"] = radioGroupNumOfBathrooms
        filterViews!!["furnished"] = radioGroupFurnished
    }

    private fun requestAdverts() {
        if (filterViews == null) return

        val checkBoxHouse            = filterViews!!["house"]     as CheckBox
        val checkBoxApartment        = filterViews!!["apartment"] as CheckBox
        val checkBoxBuy              = filterViews!!["buy"]       as CheckBox
        val checkBoxRent             = filterViews!!["rent"]      as CheckBox
        val checkBoxStructureType    = filterViews!!["structure"] as ArrayList<*>
        val checkBoxCity             = filterViews!!["city"]      as ArrayList<*>
        val radioGroupNumOfRooms     = filterViews!!["bedrooms"]  as RadioGroup
        val radioGroupNumOfBathrooms = filterViews!!["bathrooms"] as RadioGroup
        val radioGroupFurnished      = filterViews!!["furnished"] as RadioGroup

        // Apply filters where needed
        val filters = FilterArray()

        // Residence type
        val residenceType = checkBoxesAsArrayList(arrayListOf(checkBoxHouse, checkBoxApartment))
        if (residenceType.size == 1)
            filters.applyFilter(FilterArray.FilterNames.ResidenceType, ResidenceType.valueOf(residenceType[0]).ordinal)

        // Sale type
        val saleType = checkBoxesAsArrayList(arrayListOf(checkBoxBuy, checkBoxRent))
        if (saleType.size == 1)
            filters.applyFilter(FilterArray.FilterNames.SaleType, SaleType.valueOf(saleType[0]).ordinal)

        // Structure type
        val structureType = checkBoxesAsArrayList(checkBoxStructureType as ArrayList<CheckBox>)
        for (i in 0 until structureType.size)
            structureType[i] = StructureType.valueOf(structureType[i]).ordinal.toString()
        if (structureType.isNotEmpty())
            filters.applyFilter(FilterArray.FilterNames.StructureType, structureType)

        // City
        val city = checkBoxesAsArrayList(checkBoxCity as ArrayList<CheckBox>)
        if (city.isNotEmpty())
            filters.applyFilter(FilterArray.FilterNames.City, city)

        // Size
        var sendSize = false
        var sizeFrom = 0.0
        if (et_filters_size_from.text.toString().isNotBlank()) {
            sizeFrom = et_filters_size_from.text.toString().toDouble()
            sendSize = true
        }
        var sizeTo = Double.MAX_VALUE
        if (et_filters_size_to.text.toString().isNotBlank()) {
            sizeTo = et_filters_size_to.text.toString().toDouble()
            sendSize = true
        }
        if (sendSize)
            filters.applyFilter(FilterArray.FilterNames.Size, sizeFrom, sizeTo)

        // Price
        var sendPrice = false
        var priceFrom = 0.0
        if (et_filters_price_from.text.toString().isNotBlank()) {
            priceFrom = et_filters_price_from.text.toString().toDouble()
            sendPrice = true
        }
        var priceTo = Double.MAX_VALUE
        if (et_filters_price_to.text.toString().isNotBlank()) {
            priceTo = et_filters_price_to.text.toString().toDouble()
            sendPrice = true
        }
        if (sendPrice)
            filters.applyFilter(FilterArray.FilterNames.Price, priceFrom, priceTo)

        // Number of bedrooms
        if (radioGroupNumOfRooms.checkedRadioButtonId != 0) {
            val checkedRadio =
                radioGroupNumOfRooms.findViewById<RadioButton>(radioGroupNumOfRooms.checkedRadioButtonId)
            filters.applyFilter(
                FilterArray.FilterNames.NumBedrooms,
                checkedRadio.text.toString().trimEnd('+').toInt()
            )
        }

        // Number of bathrooms
        if (radioGroupNumOfBathrooms.checkedRadioButtonId != 0) {
            val checkedRadio =
                radioGroupNumOfBathrooms.findViewById<RadioButton>(radioGroupNumOfBathrooms.checkedRadioButtonId)
            filters.applyFilter(
                FilterArray.FilterNames.NumBathrooms,
                checkedRadio.text.toString().trimEnd('+').toInt()
            )
        }

        // Furnished
        if (radioGroupFurnished.checkedRadioButtonId != 0) {
            val checkedRadio =
                radioGroupFurnished.findViewById<RadioButton>(radioGroupFurnished.checkedRadioButtonId)
            when(checkedRadio.text.toString()) {
                "Da" -> filters.applyFilter(FilterArray.FilterNames.Furnished, "True")
                "Ne" -> filters.applyFilter(FilterArray.FilterNames.Furnished, "False")
            }
        }

        // make request
        performAdvertsRequest(filters.getFilters(), true)
    }

    private fun performAdvertsRequest(filterArray: String, resetToPageOne: Boolean = false) {
        val params = HashMap<String, String>()

        // send filters
        params["filterArray"] = filterArray

        // send sorting parameters
        when (sortBy.split(" ")[0]) {
            "Cena"       -> params["orderBy"] = "Price"
            "Kvadratura" -> params["orderBy"] = "Price"
            "Staros"     -> params["orderBy"] = "DateCreated"
        }
        when (sortBy.split(" ")[1]) {
            "opadajuća" -> params["ascending"] = "false"
            "rastuća"   -> params["ascending"] = "true"
        }

        // send paging parameters
        if (resetToPageOne) currentPage = 1
        params["pageNum"]    = currentPage.toString()
        params["adsPerPage"] = advertsPerPage.toString()

        // search query
        params["searchParam"] = searchQuery

        ReqSender.sendRequest(
            this.requireActivity(),
            Request.Method.POST,
            "advert/search_adverts",
            params,
            { response ->
                val advertCount = response.getInt("count")
                tv_number_of_ads.text = "$advertCount oglasa"

                numberOfPages = if (advertCount == 0) 1 else (advertCount - 1) / advertsPerPage + 1
                updatePaging()

                adverts = loadAdverts(response.getJSONArray("result"))
                advertAdapter.updateAdverts(adverts)

                // load favorites
                loadFavorites()

                fragmentState!!["filterArray"]    = filterArray
                fragmentState!!["currentPage"]    = currentPage
                fragmentState!!["numberOfPages"]  = numberOfPages
                fragmentState!!["advertsPerPage"] = advertsPerPage
                fragmentState!!["sortBy"]         = sortBy
                fragmentState!!["searchQuery"]    = searchQuery

                scv_search_scroll.scrollTo(0, 0)
            },
            { error ->
                Toast.makeText(activity, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun loadFavorites() {
        ReqSender.sendRequestArray(
            this.requireActivity(),
            Request.Method.POST,
            "advert/get_favourite_adverts",
            null,
            { response ->
                val favoriteAdverts = loadAdverts(response).map { advert -> advert.id }
                for (i in 0 until adverts.size) {
                    if (favoriteAdverts.contains(adverts[i].id)) {
                        advertAdapter.addToFavorites(i)
                    }
                }
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
         * @param advertsFilterArray Parameter 1.
         * @return A new instance of fragment SearchFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(advertsFilterArray: String) =
            SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, advertsFilterArray)
                }
            }

        var fragmentState : HashMap<String, Any>? = null
        var allCities : Array<String>? = null
    }
}