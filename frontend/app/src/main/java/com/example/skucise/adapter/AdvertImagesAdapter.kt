package com.example.skucise.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skucise.R
import kotlinx.android.synthetic.main.item_advert_image.view.*
import kotlinx.android.synthetic.main.item_advert_image2.view.*

class AdvertImagesAdapter (
    private val images: List<String> = ArrayList(),
    private val type: Int
) : RecyclerView.Adapter<AdvertImagesAdapter.AdvertImageViewHolder>() {

    inner class AdvertImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdvertImagesAdapter.AdvertImageViewHolder {
        val layout = if (type == 0) R.layout.item_advert_image else R.layout.item_advert_image2

        return AdvertImageViewHolder(LayoutInflater.from(parent.context).inflate(
                layout,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AdvertImageViewHolder, position: Int) {
        val image = images[position]
        holder.itemView.apply {
            img_advert?.clipToOutline = true
            Glide.with(context)
                .load(image)
                    .centerCrop()
                .placeholder(R.drawable.beograd)
                .into(if (type==0) img_advert else img_advert2)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }
}