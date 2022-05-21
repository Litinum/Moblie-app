package com.example.skucise.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.example.skucise.*
import com.example.skucise.Util.Companion.getFileExtension
import com.example.skucise.Util.Companion.getFileName
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.adapter.AddAdvertImagesAdapter
import com.example.skucise.fragments.SearchFragment.Companion.allCities
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.fragment_add_advert.*
import kotlinx.android.synthetic.main.fragment_add_advert.atv_city
import kotlinx.android.synthetic.main.fragment_add_advert.atv_residence_type
import kotlinx.android.synthetic.main.fragment_add_advert.atv_sale_type
import kotlinx.android.synthetic.main.fragment_add_advert.atv_structure_type
import kotlinx.android.synthetic.main.fragment_add_advert.btn_add_image
import kotlinx.android.synthetic.main.fragment_add_advert.et_description
import kotlinx.android.synthetic.main.fragment_add_advert.et_num_bathrooms
import kotlinx.android.synthetic.main.fragment_add_advert.et_num_bedrooms
import kotlinx.android.synthetic.main.fragment_add_advert.et_price
import kotlinx.android.synthetic.main.fragment_add_advert.et_size
import kotlinx.android.synthetic.main.fragment_add_advert.et_year_of_make
import kotlinx.android.synthetic.main.fragment_add_advert.rcv_advert_images
import kotlinx.android.synthetic.main.fragment_add_advert.sw_furnished
import kotlinx.android.synthetic.main.fragment_add_advert.ti_adress
import kotlinx.android.synthetic.main.fragment_add_advert.ti_title
import kotlinx.android.synthetic.main.fragment_edit_advert.*
import kotlinx.android.synthetic.main.fragment_my_account.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * A simple [Fragment] subclass.
 * Use the [AddAdvertFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddAdvertFragment : Fragment() {

    private val imageURIs = ArrayList<Uri>()

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

        if (allCities != null) {
            val cityArrayAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_add_advert, allCities!!)
            atv_city.setAdapter(cityArrayAdapter)
        }

        rcv_advert_images.adapter = AddAdvertImagesAdapter(imageURIs)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_advert, container, false)
    }

    private fun makeError(text : String){
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        atv_residence_type.setText(ResidenceType.values()[0].toString())
        atv_sale_type.setText(SaleType.values()[0].toString())
        atv_structure_type.setText(StructureType.values()[0].toString())
        if (allCities != null)
            atv_city.setText(allCities!![0])

        btn_add_image.setOnClickListener {
            galleryCheckPermission()
        }

        btn_add_advert.setOnClickListener {
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
            if(numBedrooms.isBlank()){
                makeError("Broj soba nije unet!")
                return@setOnClickListener
            }
            if(numBathrooms.isBlank()) {
                makeError("Broj kupatila nije unet!")
                return@setOnClickListener
            }

            // send request
            val js = JSONObject()
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
            params["advertJson"] = js.toString()

            val urlAddAdvert = "advert/add_advert"

            ReqSender.sendRequestString(
                context = this.requireActivity(),
                method = Request.Method.POST,
                url = urlAddAdvert,
                params = params,
                listener = { response ->
                    val advertId = response.toUInt()

                    // Don't send images
                    if(imageURIs.isEmpty()) {
                        val navigationView = requireActivity().nav_bottom_navigator

                        navigationView!!.menu.setGroupCheckable(0, true, false)
                        for (i in 0 until navigationView.menu.size()) {
                            navigationView.menu.getItem(i).isChecked = false
                        }
                        navigationView.menu.setGroupCheckable(0, true, true)

                        val args = Bundle()
                        args.putInt("advertId", advertId.toInt())
                        findNavController().navigate(R.id.advertFragment, args)

                        return@sendRequestString
                    }

                    // Send images
                    val urlAddAdvertImages = "image/add_advert_images?advertId=$advertId"

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
                            val navigationView = requireActivity().nav_bottom_navigator

                            navigationView!!.menu.setGroupCheckable(0, true, false)
                            for (i in 0 until navigationView.menu.size()) {
                                navigationView.menu.getItem(i).isChecked = false
                            }
                            navigationView.menu.setGroupCheckable(0, true, true)

                            val args = Bundle()
                            args.putInt("advertId", advertId.toInt())
                            findNavController().navigate(R.id.advertFragment, args)
                        },
                        { error ->
                            Toast.makeText(requireContext(), "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                errorListener = { error ->
                    Toast.makeText(requireContext(), "textError:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                },
                authorization = true
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            AddAdvertFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}