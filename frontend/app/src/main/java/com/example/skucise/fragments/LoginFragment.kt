package com.example.skucise.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_login.*
import org.json.JSONException
import com.android.volley.Request
import android.text.InputFilter
import com.example.skucise.*
import com.example.skucise.Util.Companion.getMessageString
import com.example.skucise.Util.Companion.startEmailAppIntent
import com.example.skucise.activities.NavigationActivity
import kotlinx.android.synthetic.main.activity_navigation.*

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var errorReport : Util.Companion.ErrorReport

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        errorReport = Util.Companion.ErrorReport(tv_login_unsuccessful)

        btn_login.setOnClickListener{
            val usernameOrEmail = et_login_username_or_email.text.toString()
            val password = et_login_password.text.toString()

            if (usernameOrEmail.isBlank()) {
                errorReport.reportError("Polje za Email/Korisničko ime je prazno!")
                return@setOnClickListener
            }
            if (password.isBlank()) {
                errorReport.reportError("Polje za Šifru ime je prazno!")
                return@setOnClickListener
            }

            val url = "login/user_login"

            val params = HashMap<String, String>()
            params["usernameOrEmail"] = usernameOrEmail
            params["password"] = password
            ReqSender.sendRequest(
                context = this.requireActivity(),
                Request.Method.POST,
                url,
                params,
                { response ->
                    try {
                        val token = response.getString("token")
                        val id = response.getInt("userId")
                        val username = response.getString("username")
                        SessionManager.startSession(token, User(id = id, username = username))

                        startActivity(Intent(this.activity, NavigationActivity::class.java))
                        this.activity?.finish()
                    } catch (e: JSONException) {
                        errorReport.reportError("json_error:\n$e")
                    }
                },
                { error ->
                    val errorMessage = error.getMessageString()
                    errorReport.reportError("error:\n$errorMessage")
                    if (errorMessage == "Vaš email nije potvrđen.") {
                        AlertDialog
                            .Builder(requireContext())
                            .setTitle("Ponovo pošalji zahtev za potvrdu?")
                            .setPositiveButton("Da") {_,_->
                                // Resend email conformation
                                ReqSender.sendRequestString(
                                    requireContext(),
                                    Request.Method.POST,
                                    "login/send_confirmation_email",
                                    hashMapOf(Pair("username", usernameOrEmail)),
                                    {
                                        startEmailAppIntent(requireActivity())
                                    },
                                    { newError ->
                                        errorReport.reportError("error:\n${newError.getMessageString()}")

                                    }
                                )

                            }
                            .setNegativeButton("Ne") {_,_->}
                            .create().show()
                    }
                },
                false
            )
        }

        // Forgot password
        btn_forgot_password.setOnClickListener {
            val usernameOrEmail = et_login_username_or_email.text.toString()

            if (usernameOrEmail.isBlank()) {
                errorReport.reportError("Polje za Email/Korisničko ime je prazno!")
                return@setOnClickListener
            }

            val url = "login/send_pass_reset_email"

            val params = HashMap<String, String>()
            params["usernameOrEmail"] = usernameOrEmail

            ReqSender.sendRequestString(
                requireContext(),
                Request.Method.POST,
                url,
                params,
                {
                    val activity = requireActivity()
                    val fragment = LoginRegisterResultsFragment(
                        mainText = "Zahtev za obnovu lozinke je poslat na vaš e-mail.\n" +
                                "Zahtev će važiti narednih 30min.",
                        positiveText = "Proveri e-mail",
                        onPositiveResponse = {
                            startEmailAppIntent(activity)
                        }
                    )

                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frc_login_or_register, fragment)
                        .commit()
                },
                { error ->
                    val errorMessage = error.getMessageString()
                    errorReport.reportError("error:\n$errorMessage")
                }
            )
        }

        et_login_username_or_email.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            source.toString().filterNot { it.isWhitespace() }
        })
        et_login_password.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            source.toString().filterNot { it.isWhitespace() }
        })
    }
}