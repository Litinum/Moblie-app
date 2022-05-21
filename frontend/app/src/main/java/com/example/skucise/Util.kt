package com.example.skucise

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.content.res.Resources.getSystem
import android.net.Uri
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.TextView
import java.io.File
import android.provider.OpenableColumns
import android.widget.Toast
import com.android.volley.VolleyError


class Util  {
    companion object {

        // Loading dialog
        class LoadingDialog(val activity: Activity) {

            @SuppressLint("InflateParams")
            private var loadingDialog = AlertDialog
                .Builder(activity)
                .setView(activity.layoutInflater.inflate(R.layout.dialog_loading, null))
                .setCancelable(false)
                .create()

            fun start() {
                loadingDialog.show()
            }

            fun dismiss() {
                loadingDialog.dismiss()
            }
        }


        // Send email
        fun startEmailAppIntent(activity: Activity) {
            val packageManager = activity.packageManager
            try {
                val intents: List<Intent> = (packageManager.queryIntentActivities(Intent(
                    Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "lowhillgamesoy@gmail.com", null
                    )
                ), 0) + packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW).also {
                    it.type = "message/rfc822"
                }, 0)).mapNotNull {
                    it.activityInfo.packageName
                }.toSet().mapNotNull {
                    packageManager.getLaunchIntentForPackage(it)
                }

                if(intents.isNotEmpty()) {
                    activity.startActivityForResult(Intent.createChooser(intents.first(), "").also {
                        if(intents.size > 1) {
                            it.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.subList(1, intents.size - 1).toTypedArray())
                        }
                    }, 1010)
                } else {
                    Toast.makeText(activity, "Potvrdite email adresu preko linka u vašem inbox-u.", Toast.LENGTH_LONG).show()
                }


            } catch (e: ActivityNotFoundException) {
                // Show error message
                e.printStackTrace()
            }
        }

        class ErrorReport constructor(private val reportTo: TextView) {
            fun reportError(message: String) {
                reportTo.text = message
                reportTo.visibility = View.VISIBLE
            }
        }


        // Information from file uri
        fun Context.getFileName(uri: Uri): String {
            var result: String? = null
            if (uri.scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.let { cursor ->
                    cursor.run {
                        if (cursor.moveToFirst()) {
                            result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        }
                    }.also { cursor.close() }
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result!!.lastIndexOf('/')
                if (cut != -1) {
                    result = result?.substring(cut + 1)
                }
            }
            return result!!
        }

        /* get actual file name or extension */
        fun Context.getFileExtension(uri: Uri): String? = when (uri.scheme) {
            // get file extension
            ContentResolver.SCHEME_FILE -> File(uri.path!!).extension
            // get actual name of file
            //ContentResolver.SCHEME_FILE -> File(uri.path!!).name
            ContentResolver.SCHEME_CONTENT -> getCursorContent(uri)
            else -> null
        }

        private fun Context.getCursorContent(uri: Uri): String? = try {
            contentResolver.query(uri, null, null, null, null)?.let { cursor ->
                cursor.run {
                    val mimeTypeMap: MimeTypeMap = MimeTypeMap.getSingleton()
                    if (moveToFirst()) mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
                    // case for get actual name of file
                    //if (moveToFirst()) getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    else null
                }.also { cursor.close() }
            }
        } catch (e: Exception) {
            null
        }

        fun VolleyError.getMessageString(): String {
            if (networkResponse == null) return ""
            return String(this.networkResponse.data, charset("utf-8"))
        }

        val Int.dp: Int get() = (this * getSystem().displayMetrics.density).toInt()
    }
}