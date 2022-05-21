package com.example.skucise.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Paint
import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.bumptech.glide.Glide
import com.example.skucise.*
import com.example.skucise.R
import com.example.skucise.SessionManager.Companion.BASE_API_URL
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.activities.AdvertImagesActivity
import com.example.skucise.adapter.ReviewAdapter
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.fragment_advert.*
import kotlinx.android.synthetic.main.item_advert.view.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "advertId"

/**
 * A simple [Fragment] subclass.
 * Use the [AdvertFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@SuppressLint("SetTextI18n")
class AdvertFragment : Fragment(), OnMapReadyCallback, TimePickerDialog.OnTimeSetListener {

    // advert data
    private var advert: Advert? = null
    private var averageScore: String = "Bez ocena"
    private var ownerUsername: String = ""
    private var isFavourite: Boolean = false

    // Calendar data
    private var selectedYear: Int = 1
    private var selectedMonth: Int = 1
    private var selectedDay: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_advert, container, false)

        // Initialize map
        val mapView = view.findViewById(R.id.map_advert_page_location) as MapView
        mapView.onCreate(null)
        mapView.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request data
        arguments?.let {
            val advertId = it.getInt(ARG_PARAM1)

            val params = HashMap<String, String>()
            params["advertId"] = advertId.toString()

            ReqSender.sendRequest(
                requireContext(),
                Request.Method.POST,
                "advert/get_advert",
                params,
                { response ->
                    val advertData = response.getJSONObject("advertData")
                    advert = Advert(
                        id                = advertData.getInt("id").toUInt(),
                        residenceType     = ResidenceType.values()[advertData.getInt("residenceType")],
                        saleType          = SaleType.values()[advertData.getInt("saleType")],
                        structureType     = StructureType.values()[advertData.getInt("structureType")],
                        title             = advertData.getString("title"),
                        description       = advertData.getString("description"),
                        city              = advertData.getString("city"),
                        address           = advertData.getString("address"),
                        size              = advertData.getDouble("size"),
                        price             = advertData.getDouble("price"),
                        ownerId           = advertData.getInt("ownerId").toUInt(),
                        numberOfBedrooms  = advertData.getInt("numBedrooms").toUInt(),
                        numberOfBathrooms = advertData.getInt("numBathrooms").toUInt(),
                        furnished         = advertData.getBoolean("furnished"),
                        yearOfMake        = advertData.getInt("yearOfMake").toUInt(),
                        dateCreated       = LocalDateTime.parse(advertData.getString("dateCreated"))
                    )
                    averageScore = response.getString("averageScore")
                    if (averageScore == "Not rated.") averageScore = "Bez ocena"
                    isFavourite = response.getBoolean("isFavourite")
                    ownerUsername = response.getString("username")

                    // update info
                    updateAdvertInfo()

                    // load reviews
                    loadReviews()

                    // load review writer
                    val canLeaveReview = response.getBoolean("canLeaveReview")
                    if(canLeaveReview) loadReviewWriter()
                },
                { error ->
                    val errorMessage = error.getMessageString()
                    Toast.makeText(requireContext(), "error:\n$errorMessage", Toast.LENGTH_LONG).show()
                }
            )

            // load advert images
            ReqSender.sendRequestArray(
                requireContext(),
                Request.Method.GET,
                "image/get_advert_image_names",
                params,
                { response ->
                    if (tv_image_counter != null)
                        tv_image_counter.text = response.length().toString()

                    if (response.length() == 0) return@sendRequestArray

                    val firstImageName = response[0].toString()
                    Glide.with(requireContext())
                        .load("${BASE_API_URL}image/get_advert_image_file?advertId=${advertId}&imageName=${firstImageName}")
                        .centerCrop()
                        .into(imv_advert_page_images)
                },
                { error ->
                    val errorMessage = error.getMessageString()
                    Toast.makeText(requireContext(), "error:\n$errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }

        // Update page look
        updateAdvertInfo()

        // Meeting arrangement
        val calender = Calendar.getInstance()
        calv_advert_page_calender.minDate = calender.timeInMillis
        calv_advert_page_calender.firstDayOfWeek = 2
        calv_advert_page_calender.setOnDateChangeListener { calendarView, year, month, day ->
            calendarView.date = System.currentTimeMillis()

            selectedYear = year
            selectedMonth = month + 1
            selectedDay = day

            val hour = calender.get(Calendar.HOUR)
            val minute = calender.get(Calendar.MINUTE)

            TimePickerDialog(requireContext(), this, hour, minute, true).show()
        }

    }

    @SuppressLint("NewApi")
    override fun onTimeSet(view: TimePicker?, hour: Int, minute: Int) {

        // inform the user that the request is being sent
        tv_advert_page_arrange_meeting_response.visibility = View.VISIBLE
        tv_advert_page_arrange_meeting_response.text = "Zahtev je salje ..."

        val time = LocalDateTime.of(selectedYear, selectedMonth, selectedDay, hour, minute)

        val params = HashMap<String, String>()
        params["advertId"] = advert!!.id.toString()
        params["time"] = time.toString()

        ReqSender.sendRequestString(
            requireContext(),
            Request.Method.POST,
            "meeting/arrange_meeting",
            params,
            {
                tv_advert_page_arrange_meeting_response.text = "Zahtev sastanka je uspešno poslat." +
                        "\nSastanak je predložen za ${time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM))}"
            },
            { error ->
                tv_advert_page_arrange_meeting_response.text = "GREŠKA: ${error.getMessageString()}"
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (map_advert_page_location != null)
            map_advert_page_location.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (map_advert_page_location != null)
            map_advert_page_location.onPause()
    }

    override fun onDestroy() {
        if (map_advert_page_location != null)
            map_advert_page_location.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (map_advert_page_location != null)
            map_advert_page_location.onLowMemory()
    }

    override fun onMapReady(map: GoogleMap) {
        with(map) {
            val position = LatLng(0.0, 0.0)
            moveCamera(CameraUpdateFactory.newLatLngZoom(position, 13f))
            addMarker(MarkerOptions().position(position))
            mapType = GoogleMap.MAP_TYPE_NORMAL
            setOnMapClickListener {
                Toast.makeText(requireContext(), "Clicked on map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("NewApi")
    private fun updateAdvertInfo() {
        if (advert == null) return

        // Some views will only be visible for advert owner
        if (SessionManager.currentUser != null && SessionManager.currentUser!!.id.toUInt() == advert?.ownerId){
            btn_add_to_favourites_advert_page.visibility = View.GONE
            btn_edit_my_advert_advert_page.visibility = View.VISIBLE
            btn_delete_my_advert_advert_page.visibility = View.VISIBLE

            btn_edit_my_advert_advert_page.setOnClickListener {
                val navigationView = requireActivity().nav_bottom_navigator

                navigationView!!.menu.setGroupCheckable(0, true, false)
                for (i in 0 until navigationView.menu.size()) {
                    navigationView.menu.getItem(i).isChecked = false
                }
                navigationView.menu.setGroupCheckable(0, true, true)

                val args = Bundle()
                args.putInt("advertId", advert!!.id.toInt())
                findNavController().navigate(R.id.editAdvertFragment, args)
            }

            btn_delete_my_advert_advert_page.setOnClickListener {
                AlertDialog
                    .Builder(requireContext())
                    .setTitle("Da li ste sigurni da želite da obrišete oglas?")
                    .setPositiveButton("Da") { _, _ ->
                        ReqSender.sendRequestString(
                            requireContext(),
                            Request.Method.POST,
                            "advert/remove_advert",
                            hashMapOf(Pair("advertId", advert!!.id.toString())),
                            {
                                requireActivity().onBackPressed()
                            },
                            { error ->
                                Toast.makeText(
                                    requireContext(),
                                    "error:\n${error.getMessageString()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                    .setNegativeButton("Ne") {_,_->}
                    .create().show()
            }

            tv_advert_page_owner.text = "@$ownerUsername (ja)"

            tv_advert_page_calender_header.visibility = View.GONE
            calv_advert_page_calender.visibility = View.GONE
        }
        else {
            btn_add_to_favourites_advert_page.visibility = View.VISIBLE
            btn_edit_my_advert_advert_page.visibility = View.GONE
            btn_delete_my_advert_advert_page.visibility = View.GONE

            // Load favorites star
            isFavouriteLoader()

            btn_add_to_favourites_advert_page.setOnClickListener {
                val action = if(isFavourite) "remove" else "add"
                ReqSender.sendRequestString(
                    requireContext(),
                    Request.Method.POST,
                    "advert/${action}_favourite_advert",
                    hashMapOf(Pair("advertId", advert!!.id.toString())),
                    {
                        isFavourite = !isFavourite
                        isFavouriteLoader()
                    },
                    { error ->
                        Toast.makeText(context, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                    }
                )
            }
            tv_advert_page_owner.text = "@$ownerUsername"

            tv_advert_page_calender_header.visibility = View.VISIBLE
            calv_advert_page_calender.visibility = View.VISIBLE
        }

        // // Visible by both owner and guests // //
        // Updating text
        tv_advert_page_sale_type.text = if (advert!!.saleType == SaleType.Prodaja) "NA PRODAJU" else "IZDAJE SE"
        tv_advert_page_title.text     = advert!!.title
        tv_advert_page_avr_rew.text   = averageScore
        tv_advert_page_date.text      = advert!!.dateCreated.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
        tv_advert_page_city.text      = "${advert!!.city},"
        tv_advert_page_address.text   = advert!!.address
        tv_advert_page_type.text      = "${advert!!.residenceType} iz ${advert!!.yearOfMake}; ${advert!!.structureType}"
        tv_advert_page_price.text     = "${advert!!.price} €"
        tv_advert_page_bedrooms.text  = "${advert!!.numberOfBedrooms} spavaće sobe"
        tv_advert_page_bathrooms.text = "${advert!!.numberOfBathrooms} kupatila"
        tv_advert_page_size.text      = "${advert!!.size} kvadrata"
        tv_advert_page_furnished.text = if (advert!!.furnished) "Sa nameštajem" else "Bez nameštaja"
        tv_advert_page_details.text   = advert!!.description

        // Updating map
        val geocoder = Geocoder(requireContext())
        val searchQuery = "${advert!!.city}, ${advert!!.address}"
        try {
            val addressList = geocoder.getFromLocationName(searchQuery, 1)
            if (addressList.size > 0) {
                val address = addressList[0]
                val position = LatLng(address.latitude, address.longitude)
                map_advert_page_location.getMapAsync { map ->
                    with(map) {
                        moveCamera(CameraUpdateFactory.newLatLngZoom(position, 13f))
                        addMarker(MarkerOptions().position(position))
                        mapType = GoogleMap.MAP_TYPE_NORMAL
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "error:\n$e", Toast.LENGTH_LONG).show()
        }

        // Owner user information
        tv_advert_page_owner.paintFlags = tv_advert_page_owner.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        tv_advert_page_owner.setOnClickListener {
            val navigationView = requireActivity().nav_bottom_navigator

            navigationView!!.menu.setGroupCheckable(0, true, false)
            for (i in 0 until navigationView.menu.size()) {
                navigationView.menu.getItem(i).isChecked = false
            }
            navigationView.menu.setGroupCheckable(0, true, true)

            val args = Bundle()
            args.putInt("userId", advert!!.ownerId.toInt())
            findNavController().navigate(R.id.myAccountFragment, args)
        }

        // See all images
        imv_advert_page_images.setOnClickListener {
            val intent = Intent(activity, AdvertImagesActivity::class.java).apply {
                putExtra("id", advert!!.id.toInt())
            }
            startActivity(intent)
        }
    }

    private fun loadReviews() {
        val params = HashMap<String, String>()
        params["advertId"] = advert!!.id.toString()

        ReqSender.sendRequestArray(
            requireContext(),
            Request.Method.POST,
            "review/get_advert_reviews",
            params,
            { response ->
                val reviews = ArrayList<Review>()
                for (i in 0 until response.length()) {
                    val review = response[i] as JSONObject
                    reviews.add(Review(
                        username = review.getString("username"),
                        rating   = review.getInt("rating"),
                        comment  = review.getString("text")
                    ))
                }

                rcv_advert_page_reviews.apply {
                    adapter = ReviewAdapter(reviews)
                    layoutManager = LinearLayoutManager(activity)
                }
            },
            { error ->
                val errorMessage = error.getMessageString()
                Toast.makeText(requireContext(), "error:\n$errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun loadReviewWriter()
    {
        cl_post_review.visibility = View.VISIBLE

        // send review
        btn_advert_page_submit_review.setOnClickListener {
            val rating = rbr_advert_page_review.progress
            val comment = et_advert_page_review_text.text.toString()

            val params = HashMap<String, String>()
            params["advertId"] = advert!!.id.toString()
            params["rating"] = rating.toString()
            params["text"] = comment

            ReqSender.sendRequestString(
                requireContext(),
                Request.Method.POST,
                "review/post_review",
                params,
                {
                    Toast.makeText(requireContext(), "Recenzija uspešno postavljena.", Toast.LENGTH_LONG).show()
                },
                { error ->
                    val errorMessage = error.getMessageString()
                    Toast.makeText(requireContext(), "error:\n$errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun isFavouriteLoader()
    {
        if (isFavourite)
        {
            btn_add_to_favourites_advert_page.setImageResource(R.drawable.ic_favourites_star_yellow_32)
            btn_add_to_favourites_advert_page.setBackgroundResource(R.drawable.shape_btn_circle_transparent_black)
        }
        else
        {
            btn_add_to_favourites_advert_page.setImageResource(R.drawable.ic_favourites_star_gray_32)
            btn_add_to_favourites_advert_page.setBackgroundResource(R.drawable.shape_btn_circle_white)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param advertId Parameter 1.
         * @return A new instance of fragment AdvertFragment.
         */
        @JvmStatic
        fun newInstance(advertId: Int) =
            AdvertFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PARAM1, advertId)
                }
            }
    }
}