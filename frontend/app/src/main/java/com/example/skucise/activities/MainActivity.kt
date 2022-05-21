package com.example.skucise.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Display
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.skucise.R
import com.example.skucise.SessionManager
import com.example.skucise.adapter.LoginScrolerAdapter
import com.example.skucise.fragments.LoginFragment
import com.example.skucise.fragments.RegisterFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var sliderHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SessionManager.loadSession(this)
        if (!SessionManager.isActive()) {
            startActivity(Intent(this, NavigationActivity::class.java))
            finish()
        }

        btn_login_fragment.setOnClickListener {
            loginButtonClicked(LoginFragment())
        }
        btn_register_fragment.setOnClickListener {
            registerButtonClicked(RegisterFragment())
        }

        // Set default fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frc_login_or_register, LoginFragment())
            .commit()

        val bg_images = arrayListOf(R.drawable.blured_login_bg1,
            R.drawable.blured_login_bg2,
            R.drawable.blured_login_bg3,
            R.drawable.blured_login_bg4)

        val display: Display = windowManager.defaultDisplay
        val screenHeight: DisplayMetrics = DisplayMetrics()
        display.getRealMetrics(screenHeight)

        //vpg_background_slider.layoutParams.height = screenHeight.heightPixels

        scv_login_background_container.setOnTouchListener { view, motionEvent -> true }
        vpg_background_slider.adapter = LoginScrolerAdapter(bg_images, vpg_background_slider, screenHeight)
        vpg_background_slider.isUserInputEnabled = false;

        sliderHandler = Handler()
        vpg_background_slider.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    sliderHandler.removeCallbacks(threadSlider)
                    sliderHandler.postDelayed(threadSlider, 3000)
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(threadSlider)
    }

    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(threadSlider, 3000)
    }

    private val threadSlider =
        Runnable {
            vpg_background_slider.currentItem = vpg_background_slider.currentItem + 1
        }

    private fun changeActiveLRFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frc_login_or_register, fragment)
        fragmentTransaction.commit()
    }

    private fun loginButtonClicked(fragment: Fragment){
        //change button color
        btn_login_fragment.background = ContextCompat.getDrawable(this,
            R.drawable.left_button_style_light
        )
        btn_login_fragment.setTextColor(ContextCompat.getColor(this, R.color.black))
        btn_register_fragment.background = ContextCompat.getDrawable(this,
            R.drawable.right_button_style_dark
        )
        btn_register_fragment.setTextColor(ContextCompat.getColor(this, R.color.transparent_white))
        changeActiveLRFragment(fragment)
    }

    private fun registerButtonClicked(fragment: Fragment){
        //change button color
        btn_login_fragment.background = ContextCompat.getDrawable(this,
            R.drawable.left_button_style_dark
        )
        btn_register_fragment.setTextColor(ContextCompat.getColor(this, R.color.black))
        btn_register_fragment.background = ContextCompat.getDrawable(this,
            R.drawable.right_button_style_light
        )
        btn_login_fragment.setTextColor(ContextCompat.getColor(this, R.color.transparent_white))
        changeActiveLRFragment(fragment)
    }
}