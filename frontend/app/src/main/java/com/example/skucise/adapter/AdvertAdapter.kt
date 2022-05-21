package com.example.skucise.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.example.skucise.*
import com.example.skucise.SessionManager.Companion.BASE_API_URL
import com.example.skucise.Util.Companion.getMessageString
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.fragment_advert.*
import kotlinx.android.synthetic.main.item_advert.*
import kotlinx.android.synthetic.main.item_advert.view.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class AdvertAdapter(
    private var adverts: ArrayList<Advert> = ArrayList(),
    private var allFavorites: Boolean = false,
    private var type: Int = 0
) : RecyclerView.Adapter<AdvertAdapter.AdvertViewHolder>() {

    class AdvertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var isFavorite: ArrayList<Boolean> = adverts.map { allFavorites } as ArrayList<Boolean>

    private var navigationView: BottomNavigationView? = null
    private lateinit var parentFragment : ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvertViewHolder {

        this.parentFragment = parent

        return AdvertViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_advert,
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: AdvertViewHolder, position: Int) {
        val currentAdvert = adverts[position]
        holder.itemView.apply {
            csl_advert_item.setOnClickListener {
                if (navigationView == null) return@setOnClickListener

                navigationView!!.menu.setGroupCheckable(0, true, false)
                for (i in 0 until navigationView!!.menu.size()) {
                    navigationView!!.menu.getItem(i).isChecked = false
                }
                navigationView!!.menu.setGroupCheckable(0, true, true)

                val args = Bundle()
                args.putInt("advertId", currentAdvert.id.toInt())
                parentFragment.findNavController().navigate(R.id.advertFragment, args)
            }
            tv_advert_title.text = currentAdvert.title
            tv_advert_date.text = currentAdvert.dateCreated.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
            tv_advert_residence_type.text = currentAdvert.residenceType.toString()
            tv_advert_city.text = "${currentAdvert.city}, ${currentAdvert.address}"
            tv_advert_type.text = currentAdvert.saleType.toString()
            tv_advert_size.text = "${currentAdvert.size} kvadrata"
            tv_advert_price.text = "${currentAdvert.price} €"


            if (SessionManager.currentUser!!.id.toUInt() == currentAdvert.ownerId){
                btn_add_to_favourites.visibility = View.GONE
                btn_delete_my_advert.visibility = View.VISIBLE
                btn_edit_my_advert.visibility = View.VISIBLE

                btn_edit_my_advert.setOnClickListener {
                    if(navigationView == null) return@setOnClickListener

                    navigationView!!.menu.setGroupCheckable(0, true, false)
                    for (i in 0 until navigationView!!.menu.size()) {
                        navigationView!!.menu.getItem(i).isChecked = false
                    }
                    navigationView!!.menu.setGroupCheckable(0, true, true)

                    val args = Bundle()
                    args.putInt("advertId", currentAdvert.id.toInt())
                    findNavController().navigate(R.id.editAdvertFragment, args)
                }
                btn_delete_my_advert.setOnClickListener {
                    AlertDialog
                        .Builder(context)
                        .setTitle("Da li ste sigurni da želite da obrišete oglas?")
                        .setPositiveButton("Da") { _, _ ->
                            ReqSender.sendRequestString(
                                context,
                                Request.Method.POST,
                                "advert/remove_advert",
                                hashMapOf(Pair("advertId", currentAdvert.id.toString())),
                                {
                                    val removePosition = adverts.indexOf(currentAdvert)
                                    adverts.removeAt(removePosition)
                                    notifyItemRemoved(removePosition)
                                },
                                { error ->
                                    Toast.makeText(context, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                        .setNegativeButton("Ne") {_,_->}
                        .create().show()
                }
            }
            else {
                btn_add_to_favourites.visibility = View.VISIBLE
                btn_delete_my_advert.visibility = View.GONE
                btn_edit_my_advert.visibility = View.GONE
            }

            if (type == 1){
                indicator_vpg.visibility = View.GONE
            }
            else {
                // favorite
                if (isFavorite[position]){
                    btn_add_to_favourites.setImageResource(R.drawable.ic_favourites_star_yellow_32)
                    btn_add_to_favourites.setBackgroundResource(R.drawable.shape_btn_circle_transparent_black)
                }
                else{
                    btn_add_to_favourites.setImageResource(R.drawable.ic_favourites_star_gray_32)
                    btn_add_to_favourites.setBackgroundResource(R.drawable.shape_btn_circle_white)
                }

                btn_add_to_favourites.setOnClickListener {
                    val action = if(isFavorite[position]) "remove" else "add"
                    ReqSender.sendRequestString(
                        context,
                        Request.Method.POST,
                        "advert/${action}_favourite_advert",
                        hashMapOf(Pair("advertId", currentAdvert.id.toString())),
                        {
                            isFavorite[position] = !isFavorite[position]
                            notifyItemChanged(position)
                        },
                        { error ->
                            Toast.makeText(context, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            var images = currentAdvert.images.map { image ->
                "${BASE_API_URL}image/get_advert_image_file?advertId=${currentAdvert.id}&imageName=$image"
            }
            if (images.isEmpty())
                images = arrayListOf("https://www.in4s.net/wp-content/uploads/2020/07/Beograd.jpg")

            val adapter = AdvertImagesAdapter(images, 0)
            vpg_advert_images.adapter = adapter
            vpg_advert_images.offscreenPageLimit = 2
            indicator_vpg.setViewPager(vpg_advert_images)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAdverts(adverts: ArrayList<Advert>) {
        this.adverts = adverts
        isFavorite = adverts.map { allFavorites } as ArrayList<Boolean>
        notifyDataSetChanged()
    }

    fun setupNavMenu(bottomNavigationView: BottomNavigationView) {
        this.navigationView = bottomNavigationView
    }

    fun addToFavorites(position: Int) {
        isFavorite[position] = true
        notifyItemChanged(position)
    }

    override fun getItemCount(): Int {
        return adverts.size
    }
}