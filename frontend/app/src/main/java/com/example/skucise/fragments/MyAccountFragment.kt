package com.example.skucise.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.skucise.*
import com.example.skucise.SessionManager.Companion.BASE_API_URL
import com.example.skucise.Util.Companion.dp
import com.example.skucise.Util.Companion.getFileExtension
import com.example.skucise.Util.Companion.getFileName
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.Util.Companion.startEmailAppIntent
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.fragment_my_account.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.HashMap
import android.util.Base64.encodeToString


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "userId"

/**
 * A simple [Fragment] subclass.
 * Use the [MyAccountFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyAccountFragment : Fragment() {

    private var user: User? = null

    val loadImageFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data

            val inputStream = requireActivity().contentResolver.openInputStream(uri!!)
            val contents = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getEncoder().encodeToString(inputStream!!.readBytes())
            } else {
                encodeToString(inputStream!!.readBytes(), 0)
            }


            val image = FileData(
                Name = requireContext().getFileName(uri),
                Extension = requireContext().getFileExtension(uri)!!,
                Content = contents
            )

            ReqSender.sendImage(
                requireContext(),
                Request.Method.PUT,
                "image/set_user_image",
                image,
                {
                    Glide.with(requireContext())
                        .load(uri)
                        .centerCrop()
                        .signature(ObjectKey(System.currentTimeMillis().toString()))
                        .into(img_user_pfp)

                    val accountPfp = requireActivity().btn_account_dd_toggle
                    accountPfp.clipToOutline = true
                    if (SessionManager.currentUser != null)
                        Glide.with(requireContext())
                            .load(uri)
                            .centerCrop()
                            .signature(ObjectKey(System.currentTimeMillis().toString()))
                            .into(accountPfp)
                },
                { error ->
                    Toast.makeText(requireContext(), "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private var userIdRequired: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { userIdRequired = it.getInt(ARG_PARAM1) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_account, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (userIdRequired == null)
            ReqSender.sendRequest(
                requireContext(),
                Request.Method.POST,
                "users/get_my_info",
                null,
                { response ->

                    user = User (
                        id              = response.getInt("id"),
                        username        = response.getString("username"),
                        firstname       = response.getString("firstName"),
                        lastname        = response.getString("lastName"),
                        email           = response.getString("email"),
                        creationDate    = LocalDateTime.parse(response.getString("dateCreated")),
                        numberOfAdverts = response.getInt("numberOfAdverts"),
                        averageRating   = response.getString("userScore")
                    )

                    loadPageData()
                } ,
                { error ->
                    val errorMessage = error.getMessageString()
                    Toast.makeText(context, "error:\n$errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        else
            ReqSender.sendRequest(
                requireContext(),
                Request.Method.POST,
                "users/get_user_info",
                hashMapOf(Pair("idUser", userIdRequired.toString())),
                { response ->
                    user = User (
                        id              = response.getInt("id"),
                        username        = response.getString("username"),
                        firstname       = response.getString("firstName"),
                        lastname        = response.getString("lastName"),
                        numberOfAdverts = response.getInt("numberOfAdverts"),
                        averageRating   = response.getString("userScore")
                    )
                    loadPageData()
                },
                { error ->
                    val errorMessage = error.getMessageString()
                    Toast.makeText(context, "error:\n$errorMessage", Toast.LENGTH_LONG).show()
                }
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadPageData() {
        if (user == null) return

        // Update text
        if (userIdRequired == null) {
            et_user_username.setText(user!!.username)
            et_user_firstname.setText(user!!.firstname)
            et_user_lastname.setText(user!!.lastname)
            tv_user_number_of_adverts.text = "Objavljenih oglasa: ${user!!.numberOfAdverts}"
            tv_user_average_rating.text = user!!.averageRating
            et_user_date_created.setText(
                user!!.creationDate.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                )
            )
            et_user_email.setText(user!!.email)

            tv_profile_date.visibility = View.VISIBLE
            tv_profile_email.visibility = View.VISIBLE
            tv_profile_email.text = user!!.email
            val date =
                "" + user!!.creationDate.dayOfMonth + "." + user!!.creationDate.monthValue + "." + user!!.creationDate.year
            tv_profile_date.text = date

            // Change profile picture
            btn_edit_profile_picture.visibility = View.VISIBLE
            btn_edit_profile_picture.setOnClickListener {
                galleryCheckPermission()
            }
            btn_edit_user.setOnClickListener {
                csl_profile_edit_container.visibility = View.VISIBLE
            }
            btn_end_editing.setOnClickListener {
                csl_profile_edit_container.visibility = View.GONE
            }

            // Setup edit user
            val defaultUsername = user!!.username
            btn_user_edit_username.setOnClickListener {
                et_user_username.isEnabled = true
                btn_user_edit_username.isEnabled = false
                btn_user_edit_cancel.isEnabled = true
                btn_user_edit_confirm.isEnabled = true
            }
            val defaultFirstname = user!!.firstname
            btn_user_edit_firstname.setOnClickListener {
                et_user_firstname.isEnabled = true
                btn_user_edit_firstname.isEnabled = false
                btn_user_edit_cancel.isEnabled = true
                btn_user_edit_confirm.isEnabled = true
            }
            val defaultLastname = user!!.lastname
            btn_user_edit_lastname.setOnClickListener {
                et_user_lastname.isEnabled = true
                btn_user_edit_lastname.isEnabled = false
                btn_user_edit_cancel.isEnabled = true
                btn_user_edit_confirm.isEnabled = true
            }

            btn_user_edit_cancel.setOnClickListener {
                et_user_username.setText(defaultUsername)
                et_user_firstname.setText(defaultFirstname)
                et_user_lastname.setText(defaultLastname)
                et_user_username.isEnabled = false
                et_user_firstname.isEnabled = false
                et_user_lastname.isEnabled = false
                btn_user_edit_username.isEnabled = true
                btn_user_edit_firstname.isEnabled = true
                btn_user_edit_lastname.isEnabled = true
                btn_user_edit_cancel.isEnabled = false
                btn_user_edit_confirm.isEnabled = false
            }
            btn_user_edit_confirm.setOnClickListener {
                val params: HashMap<String, String> = HashMap()
                params["newUsername"]  = et_user_username.text.toString()
                params["newFirstName"] = et_user_firstname.text.toString()
                params["newLastName"]  = et_user_lastname.text.toString()

                ReqSender.sendRequestString(
                    requireContext(),
                    Request.Method.POST,
                    "users/change_user_info",
                    params,
                    {
                        if (et_user_username.isEnabled) {
                            val newUsername = et_user_username.text.toString()
                            SessionManager.changeUsername(newUsername)
                            requireActivity().tv_account_dd_username.text = newUsername
                        }

                        et_user_username.isEnabled = false
                        et_user_firstname.isEnabled = false
                        et_user_lastname.isEnabled = false
                        btn_user_edit_username.isEnabled = true
                        btn_user_edit_firstname.isEnabled = true
                        btn_user_edit_lastname.isEnabled = true
                        btn_user_edit_cancel.isEnabled = false
                        btn_user_edit_confirm.isEnabled = false
                    } ,
                    { error ->
                        Toast.makeText(context, "error:\n${error.getMessageString()}", Toast.LENGTH_LONG).show()
                    }
                )
            }

            // Edit email
            btn_user_edit_mail.setOnClickListener {
                val newEmailView = EditText(requireContext())
                val newEmailViewR = EditText(requireContext())
                newEmailView.hint = "Novi e-mail..."
                newEmailViewR.hint = "Ponovi novi e-mail..."
                val linearLayout = LinearLayout(requireContext())

                linearLayout.orientation = LinearLayout.VERTICAL
                linearLayout.addView(newEmailView)
                linearLayout.addView(newEmailViewR)

                // New mail form
                val newEmailDialog = AlertDialog
                    .Builder(requireContext())
                    .setPositiveButton("Pošalji zahtev") { _, _ ->
                        val newEmail = newEmailView.text.toString()
                        val newEmailR = newEmailViewR.text.toString()

                        // Check inputs
                        if (newEmail.isBlank() || newEmailR.isBlank()) {
                            AlertDialog
                                .Builder(requireContext())
                                .setMessage("Sva polja moraju biti popunjena.")
                                .create()
                                .show()
                            return@setPositiveButton
                        }
                        if (newEmail != newEmailR) {
                            AlertDialog
                                .Builder(requireContext())
                                .setMessage("Email nije dobro ponovljen.")
                                .create()
                                .show()
                            return@setPositiveButton
                        }
                        if (!Regex("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$").matches(newEmail)) {
                            AlertDialog
                                .Builder(requireContext())
                                .setMessage("Forma email adrese je pogrešna.")
                                .create()
                                .show()
                            return@setPositiveButton
                        }

                        // Send new mail
                        ReqSender.sendRequestString(
                            requireContext(),
                            Request.Method.POST,
                            "users/change_email",
                            hashMapOf(Pair("newEmail", newEmail)),
                            {
                                // Confirm mail prompt
                                AlertDialog
                                    .Builder(requireContext())
                                    .setMessage("Kako bi dovršili promenu e-maila potvrdite novu e-mail adresu.\n" +
                                            "Potvrda će biti moguća narednih 30 min.")
                                    .setPositiveButton("Potvrdi novi E-mail") { _, _ ->
                                        startEmailAppIntent(requireActivity())
                                    }
                                    .create()
                                    .show()
                            },
                            { error ->
                                val errorString = error.getMessageString()
                                Toast.makeText(requireContext(), "error\n$errorString", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                    .setNegativeButton("Poništi") {_,_->}
                    .create()
                newEmailDialog.setView(linearLayout, 16.dp, 16.dp, 16.dp, 0)
                newEmailDialog.show()
            }

            // Password
            btn_user_edit_password.setOnClickListener {
                val oldPasswordView  = EditText(requireContext())
                val newPasswordView  = EditText(requireContext())
                val newPasswordRView = EditText(requireContext())
                oldPasswordView.hint  = "Stara Lozinka..."
                newPasswordView.hint  = "Nova Lozinka..."
                newPasswordRView.hint = "Ponovi Novu Lozinku..."
                oldPasswordView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                newPasswordView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                newPasswordRView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                val linearLayout = LinearLayout(requireContext())
                linearLayout.orientation = LinearLayout.VERTICAL
                linearLayout.addView(oldPasswordView)
                linearLayout.addView(newPasswordView)
                linearLayout.addView(newPasswordRView)

                val changePasswordDialog = AlertDialog
                    .Builder(requireContext())
                    .setPositiveButton("Promeni šifru") { _, _ ->
                        val oldPassword  = oldPasswordView.text.toString()
                        val newPassword  = newPasswordView.text.toString()
                        val newPasswordR = newPasswordRView.text.toString()

                        // Check if inputs are proper
                        if (oldPassword.isBlank() || newPassword.isBlank() || newPasswordR.isBlank()) {
                            AlertDialog
                                .Builder(requireContext())
                                .setMessage("Sva polja moraju biti popunjena.")
                                .create()
                                .show()
                            return@setPositiveButton
                        }

                        if (newPasswordR != newPassword) {
                            AlertDialog
                                .Builder(requireContext())
                                .setMessage("Šifra nije dobro ponovljena.")
                                .create()
                                .show()
                            return@setPositiveButton
                        }

                        if (!Regex("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9]).{8,32}$").matches(newPasswordR)) {
                            AlertDialog
                                .Builder(requireContext())
                                .setMessage("Šifra mora da sadrži makar jedno malo i veliko slovo. broj, i svega od 8 do 32 karaktera")
                                .create()
                                .show()
                            return@setPositiveButton
                        }

                        // Send password change request
                        val params = hashMapOf<String, String>()
                        params["oldPassword"] = oldPassword
                        params["newPassword"] = newPassword

                        ReqSender.sendRequestString(
                            requireContext(),
                            Request.Method.POST,
                            "users/change_password",
                            params,
                            {
                                // Confirm password changed
                                AlertDialog
                                    .Builder(requireContext())
                                    .setMessage("Promena šifre je uspešno izvršena.")
                                    .create()
                                    .show()
                            },
                            { error ->
                                val errorString = error.getMessageString()
                                AlertDialog
                                    .Builder(requireContext())
                                    .setMessage("error:\n$errorString")
                                    .create()
                                    .show()
                            }
                        )
                    }
                    .setNegativeButton("Poništi") {_,_->}
                    .create()
                changePasswordDialog.setView(linearLayout,  16.dp,16.dp, 16.dp, 16.dp)
                changePasswordDialog.show()
            }

            // Enter any edits
            btn_edit_user.visibility = View.VISIBLE
        }
        else {
            tv_profile_date.visibility = View.GONE
            tv_profile_email.visibility = View.GONE

            tv_date_label.visibility = View.GONE
            tv_email_label.visibility = View.GONE

            btn_edit_profile_picture.visibility = View.GONE

            btn_edit_user.visibility = View.GONE
        }

        // Other text
        tv_profile_username.text = user!!.username
        val fullname = user!!.firstname + " " + user!!.lastname
        tv_profile_name.text = fullname
        tv_profile_number_adverts.text = user!!.numberOfAdverts.toString()
        tv_average_rating.text = user!!.averageRating

        // Update profile picture
        img_user_pfp.clipToOutline = true
        Glide.with(this)
            .load("${BASE_API_URL}image/get_user_image_file?userId=${user!!.id}")
            .centerCrop()
            .signature(ObjectKey(System.currentTimeMillis().toString()))
            .into(img_user_pfp)
    }

    private fun galleryCheckPermission() {
        Dexter.withContext(requireContext())
            .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param userId Parameter 1.
         * @return A new instance of fragment MyAccountFragment.
         */
        @JvmStatic
        fun newInstance(userId: Int) =
            MyAccountFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PARAM1, userId)
                }
            }
    }
}