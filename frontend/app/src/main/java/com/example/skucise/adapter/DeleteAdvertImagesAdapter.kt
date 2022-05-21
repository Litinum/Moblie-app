package com.example.skucise.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.bumptech.glide.Glide
import com.example.skucise.FileData
import com.example.skucise.R
import com.example.skucise.ReqSender
import com.example.skucise.SessionManager.Companion.BASE_API_URL
import com.example.skucise.Util.Companion.getMessageString
import kotlinx.android.synthetic.main.item_add_advert_image.view.*
import kotlinx.android.synthetic.main.item_advert_image.view.*
import kotlinx.android.synthetic.main.item_advert_image.view.img_advert
import kotlinx.android.synthetic.main.item_advert_image2.view.*

class DeleteAdvertImagesAdapter (
    private val advertId: Int,
    private val imageNames: ArrayList<String> = ArrayList()
) : RecyclerView.Adapter<DeleteAdvertImagesAdapter.DeleteAdvertImageViewHolder>() {

    inner class DeleteAdvertImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    val imagesToDelete = ArrayList<String>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeleteAdvertImagesAdapter.DeleteAdvertImageViewHolder {
        //val layout = if (type == 0) R.layout.item_advert_image else R.layout.item_advert_image2

        return DeleteAdvertImageViewHolder(LayoutInflater.from(parent.context).inflate(
                R.layout.item_add_advert_image,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: DeleteAdvertImageViewHolder, position: Int) {
        val image = imageNames[position]
        holder.itemView.apply {
            btn_remove_image.setOnClickListener {
                val img = imageNames[position]
                imagesToDelete.add(img)
                imageNames.remove(img)
                notifyDataSetChanged()
            }
            img_advert?.clipToOutline = true
            Glide.with(context)
                .load("${BASE_API_URL}image/get_advert_image_file?advertId=$advertId&imageName=$image")
                    .centerCrop()
                .placeholder(R.drawable.beograd)
                .into(img_advert)
        }
    }

    override fun getItemCount(): Int {
        return imageNames.size
    }
}