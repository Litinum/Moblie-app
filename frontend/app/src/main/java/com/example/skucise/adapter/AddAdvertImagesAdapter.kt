package com.example.skucise.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skucise.FileData
import com.example.skucise.R
import kotlinx.android.synthetic.main.item_add_advert_image.view.*
import kotlinx.android.synthetic.main.item_advert_image.view.*
import kotlinx.android.synthetic.main.item_advert_image.view.img_advert
import kotlinx.android.synthetic.main.item_advert_image2.view.*

class AddAdvertImagesAdapter (
    private val imageURIs: ArrayList<Uri> = ArrayList(),
) : RecyclerView.Adapter<AddAdvertImagesAdapter.AddAdvertImageViewHolder>() {

    inner class AddAdvertImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddAdvertImagesAdapter.AddAdvertImageViewHolder {
        //val layout = if (type == 0) R.layout.item_advert_image else R.layout.item_advert_image2

        return AddAdvertImageViewHolder(LayoutInflater.from(parent.context).inflate(
                R.layout.item_add_advert_image,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AddAdvertImageViewHolder, position: Int) {
        val image = imageURIs[position]
        holder.itemView.apply {
            btn_remove_image.setOnClickListener {
                imageURIs.removeAt(holder.adapterPosition)
                notifyItemRemoved(holder.adapterPosition)
            }
            img_advert?.clipToOutline = true
            Glide.with(context)
                .load(image)
                    .centerCrop()
                .placeholder(R.drawable.beograd)
                .into(img_advert)
        }
    }

    override fun getItemCount(): Int {
        return imageURIs.size
    }
}