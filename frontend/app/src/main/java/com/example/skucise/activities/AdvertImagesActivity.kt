package com.example.skucise.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.example.skucise.R
import com.example.skucise.ReqSender
import com.example.skucise.adapter.AdvertImagesAdapter
import kotlinx.android.synthetic.main.activity_advert_images.*
import com.example.skucise.SessionManager.Companion.BASE_API_URL
import com.example.skucise.Util.Companion.getMessageString

class AdvertImagesActivity : AppCompatActivity() {

    private var imageNames = ArrayList<String>()
    private var imageUrls = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advert_images)

        val advertId = intent.getIntExtra("id", -1)

        val params = HashMap<String, String>()
        params["advertId"] = advertId.toString()

        ReqSender.sendRequestArray(
            this,
            Request.Method.GET,
            "image/get_advert_image_names",
            params,
            { response ->
                imageNames = ArrayList()
                imageUrls = ArrayList()
                for (i in 0 until response.length()) {
                    val splits = response[i].toString()
                    imageNames.add(splits)
                    imageUrls.add("${BASE_API_URL}image/get_advert_image_file?advertId=${advertId}&imageName=$splits")
                }
                rcv_advert_images.isNestedScrollingEnabled = false
                rcv_advert_images.adapter = AdvertImagesAdapter(imageUrls, 2)
                rcv_advert_images.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            },
            { error ->
                Toast.makeText(this, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
            }
        )
    }
}