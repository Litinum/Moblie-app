package com.example.skucise.fragments

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.skucise.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_login_register_results.*

class LoginRegisterResultsFragment(
    private val mainText: String,
    private val positiveText: String? = null,
    private val onPositiveResponse: View.OnClickListener = View.OnClickListener {},
    private val negativeText: String? = null,
    private val onNegativeResponse: View.OnClickListener = View.OnClickListener {}
) : Fragment(R.layout.fragment_login_register_results) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // disable other two buttons
        val activity = requireActivity()
        activity.btn_login_fragment.background = ContextCompat.getDrawable(activity,
            R.drawable.left_button_style_dark
        )
        activity.btn_login_fragment.setTextColor(ContextCompat.getColor(activity, R.color.transparent_white))
        activity.btn_register_fragment.background = ContextCompat.getDrawable(activity,
            R.drawable.right_button_style_dark
        )
        activity.btn_register_fragment.setTextColor(ContextCompat.getColor(activity, R.color.transparent_white))

        // set main text info
        tv_info_text.text = mainText

        // set positive response info text
        if (positiveText != null) {
            btn_info_positive.text = positiveText
            btn_info_positive.visibility = View.VISIBLE
            btn_info_positive.setOnClickListener(onPositiveResponse)
        }
        else
            btn_info_positive.visibility = View.INVISIBLE

        // set negative response info text
        if (negativeText != null) {
            btn_info_negative.text = negativeText
            btn_info_negative.visibility = View.VISIBLE
            btn_info_negative.setOnClickListener(onNegativeResponse)
        }
        else
            btn_info_negative.visibility = View.INVISIBLE
    }
}