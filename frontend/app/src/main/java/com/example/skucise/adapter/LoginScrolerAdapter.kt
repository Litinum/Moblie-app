package com.example.skucise.adapter

import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.skucise.R
import kotlinx.android.synthetic.main.login_image_item.view.*
import kotlinx.coroutines.Runnable

class LoginScrolerAdapter (
    private val images: ArrayList<Int> = ArrayList(),
    private val viewPager: ViewPager2,
    private val screenHeight: DisplayMetrics
) : RecyclerView.Adapter<LoginScrolerAdapter.LoginImageViewHolder>(){

    inner class LoginImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginImageViewHolder {
        return LoginImageViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.login_image_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LoginImageViewHolder, position: Int) {
        val image = images[position]
        holder.itemView.apply {
            img_login_background.layoutParams.height = screenHeight.heightPixels
            img_login_background.setImageResource(image)
            img_login_background.scaleType = ImageView.ScaleType.CENTER_CROP

            if (position == images.size - 2){
                viewPager.post(run)
            }
        }
    }

    private val run = Runnable {
        images.addAll(images)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return images.size
    }
}