package com.example.skucise.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.example.skucise.*
import com.example.skucise.Util.Companion.getFileExtension
import com.example.skucise.Util.Companion.getFileName
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.adapter.AddAdvertImagesAdapter
import com.example.skucise.adapter.DeleteAdvertImagesAdapter
import com.example.skucise.fragments.SearchFragment.Companion.allCities
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.fragment_edit_advert.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "advertId"


class EditAdvertFragment : Fragment() {

    private val imageURIs = ArrayList<Uri>()
    private val imageNames = ArrayList<String>()
    private lateinit var oldAdapter: DeleteAdvertImagesAdapter
    private var advertId : Int = 0
    private var advert : Advert? = null

    val loadImageFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data?.clipData != null) {
                val uris = result.data?.clipData

                for (i in 0 until uris!!.itemCount) {
                    val uri = (uris.getItemAt(i).uri)
                    var contains = false
                    for (imgURI in imageURIs) {
                        if (requireContext().getFileName(imgURI) == requireContext().getFileName(uri))
                            contains = true
                    }
                    if (!contains)
                        imageURIs.add(uri)
                }
            }
            else {
                val uri = result.data?.data?: return@registerForActivityResult

                var contains = false
                for (imgURI in imageURIs) {
                    if (requireContext().getFileName(imgURI) == requireContext().getFileName(uri))
                        contains = true
                }
                if (!contains)
                    imageURIs.add(uri)
            }
        }
    }

    private fun showRotationalDialogForPermission() {
        AlertDialog.Builder(requireContext())
            .setMessage("Dozvola za pristup neophodna.")
            .setPositiveButton("Podešavanja") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", requireActivity().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
    }

    private fun galleryCheckPermission() {
        Dexter.withContext(requireContext())
            .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    loadImageFromGallery.launch(intent)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        requireContext(),
                        "You have denied the storage permission to select image",
                        Toast.LENGTH_SHORT
                    ).show()
                    showRotationalDialogForPermission()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    showRotationalDialogForPermission()
                }

            }).onSameThread().check()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            advertId = it.getInt(ARG_PARAM1)

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

                    val images = response.getJSONArray("images")
                    for(i in 0 until images.length()){
                        imageNames.add(images[i].toString())
                    }

                    if(rcv_advert_images_old.adapter != null){
                        rcv_advert_images_old.adapter!!.notifyDataSetChanged()
                    }
//                  updateAdvertInfo()

                    ti_title.setText(advert!!.title)
                    et_description.setText(advert!!.description)
                    ti_adress.setText(advert!!.address)
                    et_size.setText(advert!!.size.toString())
                    et_price.setText(advert!!.price.toString())
                    et_num_bedrooms.setText(advert!!.numberOfBedrooms.toString())
                    et_num_bathrooms.setText(advert!!.numberOfBathrooms.toString())
                    et_year_of_make.setText(advert!!.yearOfMake.toString())
                    if(advert!!.furnished)
                        sw_furnished.isChecked = true
                    atv_residence_type.setText(advert!!.residenceType.toString())
                    atv_sale_type.setText(advert!!.saleType.toString())
                    atv_structure_type.setText(advert!!.structureType.toString())
                    atv_city.setText(advert!!.city)

                },
                { error ->
                    Toast.makeText(activity, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                }
            )

//            ReqSender.sendRequestArray(
//                requireContext(),
//                Request.Method.GET,
//                "image/get_advert_image_names",
//                params,
//                { response ->
//                    var images = ArrayList<String>()
//
//                    for (i in 0 until response.length())
//                    {
//                        val image = response[i] as String
//                        imageURLs.add (URL("image/get_advert_image_file?advertId=${advertId}&imageName=$image"))
//                    }
//
//                    if (images.isEmpty())
//                        images = arrayListOf("https://www.in4s.net/wp-content/uploads/2020/07/Beograd.jpg")
//
//                    val adapter = AdvertImagesAdapter(images, 0)
//
//
//                },
//                { error ->
//                    Toast.makeText(requireContext(), "error:\{error.getMessageString()}", Toast.LENGTH_LONG).show()
//                }
//            )
        }
    }

    override fun onResume() {
        super.onResume()
        val residenceTypeArrayAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_add_advert, ResidenceType.values())
        atv_residence_type.setAdapter(residenceTypeArrayAdapter)

        val saleTypeArrayAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_add_advert, SaleType.values())
        atv_sale_type.setAdapter(saleTypeArrayAdapter)

        val structureTypeArrayAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_add_advert, StructureType.values())
        atv_structure_type.setAdapter(structureTypeArrayAdapter)

        val cities = allCities!!
        val cityArrayAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_add_advert, cities)
        atv_city.setAdapter(cityArrayAdapter)

        rcv_advert_images.adapter = AddAdvertImagesAdapter(imageURIs)

        oldAdapter = DeleteAdvertImagesAdapter(advertId, imageNames)
        rcv_advert_images_old.adapter = oldAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_advert, container, false)
    }

    private fun makeError(text : String){
        Toast.makeText(requireContext(), "$text", Toast.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_add_image.setOnClickListener {
            galleryCheckPermission()
        }

        btn_confirm_changes.setOnClickListener {
            val residenceType = ResidenceType.valueOf(atv_residence_type.text.toString()).ordinal
            val structureType = StructureType.valueOf(atv_structure_type.text.toString()).ordinal
            val saleType = SaleType.valueOf(atv_sale_type.text.toString()).ordinal
            val furnished = sw_furnished.isChecked
            val city = atv_city.text.toString()
            val address = ti_adress.text.toString()
            val title = ti_title.text.toString()
            val description = et_description.text.toString()
            val yearOfMake = et_year_of_make.text.toString()
            val size = et_size.text.toString()
            val price = et_price.text.toString()
            val numBedrooms = et_num_bedrooms.text.toString()
            val numBathrooms = et_num_bathrooms.text.toString()

            if(title.isBlank()){
                makeError("Naslov ne može biti prazan!")
                return@setOnClickListener
            }
            if(description.isBlank()){
                makeError("Opis nije unet!")
                return@setOnClickListener
            }
            if(address.isBlank()){
                makeError("Adresa nije uneta!")
                return@setOnClickListener
            }
            if(size.isBlank()){
                makeError("Kvadratura nije uneta!")
                return@setOnClickListener
            }
            if(price.isBlank()){
                makeError("Cena nije uneta!")
                return@setOnClickListener
            }
            if(yearOfMake.isBlank()){
                makeError("Godina gradnje nije uneta!")
                return@setOnClickListener
            }
            if(yearOfMake.isBlank()){
                makeError("Godina gradnje nije uneta!")
                return@setOnClickListener
            }
            if(numBedrooms.isBlank()){
                makeError("Broj soba nije unet!")
                return@setOnClickListener
            }
            if(numBathrooms.isBlank()) {
                makeError("Broj kupatila nije unet!")
                return@setOnClickListener
            }


            val loadingDialog = AlertDialog
                .Builder(requireContext())
                .setView(requireActivity().layoutInflater.inflate(R.layout.dialog_loading, null))
                .setCancelable(false)
                .create()
            loadingDialog.show()

            val js = JSONObject()
            js.put("Id", advert!!.id.toInt())
            js.put("DateCreated", advert!!.dateCreated)
            js.put("OwnerId", advert!!.ownerId.toInt())
            js.put("ResidenceType", residenceType)
            js.put("StructureType", structureType )
            js.put("SaleType", saleType)
            js.put("Furnished", furnished)
            js.put("City", city)
            js.put("Address", address)
            js.put("Title",title)
            js.put("Description", description)
            js.put("YearOfMake", yearOfMake.toInt())
            js.put("Size",size.toDouble())
            js.put("Price", price.toDouble())
            js.put("NumBedrooms", numBedrooms.toInt())
            js.put("NumBathrooms", numBathrooms.toInt())

            val params = HashMap<String, String>()
            params["editJson"] = js.toString()

            val urlEditAdvert = "advert/edit_advert"

            ReqSender.sendRequestString(
                context = this.requireActivity(),
                method = Request.Method.POST,
                url = urlEditAdvert,
                params = params,
                listener = { response ->
                    Toast.makeText(requireContext(), oldAdapter.imagesToDelete.joinToString(","), Toast.LENGTH_SHORT).show()
                    if(oldAdapter.imagesToDelete.isNotEmpty()){
                        for(img in oldAdapter.imagesToDelete) {
                            val url =
                                "image/delete_advert_image?advertId=$advertId&imageName=$img"

                            ReqSender.sendRequestString(
                                requireContext(),
                                Request.Method.DELETE,
                                url,
                                null,
                                { },
                                { error ->
                                    Toast.makeText(
                                        context,
                                        "error:\n${error.getMessageString()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                loadingScreen = false
                            )
                        }
                        oldAdapter.imagesToDelete.clear()
                    }

                    if(imageURIs.isEmpty())
                    {
                        val args = Bundle()
                        args.putInt("advertId", advert!!.id.toInt())
                        findNavController().navigate(R.id.advertFragment, args)
                        loadingDialog.dismiss()
                        return@sendRequestString
                    }

                    val urlAddAdvertImages = "image/add_advert_images?advertId=${advert!!.id}"

                    val images = ArrayList<FileData>()

                    for(uri in imageURIs){
                        val inputStream = requireActivity().contentResolver.openInputStream(uri)
                        val contents = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Base64.getEncoder().encodeToString(inputStream!!.readBytes())
                        } else {
                            android.util.Base64.encodeToString(inputStream!!.readBytes(), 0)
                        }

                        val image = FileData(
                            Name = requireContext().getFileName(uri),
                            Extension = requireContext().getFileExtension(uri)!!,
                            Content = contents
                        )

                        images.add(image)
                    }

                    ReqSender.sendImage(
                        requireContext(),
                        Request.Method.PUT,
                        urlAddAdvertImages,
                        images,
                        {
                            Toast.makeText(requireContext(), "response:\n$response", Toast.LENGTH_LONG).show()

                            val navigationView = requireActivity().nav_bottom_navigator

                            navigationView!!.menu.setGroupCheckable(0, true, false)
                            for (i in 0 until navigationView.menu.size()) {
                                navigationView.menu.getItem(i).isChecked = false
                            }
                            navigationView.menu.setGroupCheckable(0, true, true)

                            val args = Bundle()
                            args.putInt("advertId", advertId.toInt())
                            findNavController().navigate(R.id.advertFragment, args)
                            loadingDialog.dismiss()
                        },
                        { error ->
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                errorListener = { error ->
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "textError:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                },
                authorization = true
            )
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