package com.example.skucise.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skucise.R
import com.example.skucise.Review
import kotlinx.android.synthetic.main.item_review.view.*

class ReviewAdapter(
    private val reviews: ArrayList<Review>
): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        return ReviewViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_review,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val currentReview = reviews[position]

        holder.itemView.apply {
            tv_review_from.text    = currentReview.username
            rbr_advert_page_review.rating = currentReview.rating.toFloat()
            tv_review_comment.text = currentReview.comment
        }
    }

    override fun getItemCount(): Int {
        return reviews.size
    }
}