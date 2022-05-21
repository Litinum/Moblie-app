package com.example.skucise

import android.app.Activity
import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import com.example.skucise.SessionManager.Companion.BASE_API_URL
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

open class ReqSender {
    companion object {
        private val queuedRequests : HashMap<Context, RequestQueue> = HashMap()

        private fun getRequestQueue(context: Context): RequestQueue {
            if (!queuedRequests.containsKey(context)) {
                queuedRequests[context] = Volley.newRequestQueue(context)
            }
            return queuedRequests[context]!!
        }

        private fun buildUrl(base_url: String, params: MutableMap<String, String>?): String {
            var url = "$BASE_API_URL$base_url?"
            if (params != null) {
                for (param in params) {
                    val paramValue = URLEncoder.encode(param.value, "utf-8")
                    url = "$url${param.key}=$paramValue&"
                }
            }
            return url.substring(0, url.length - 1)
        }

        fun sendRequest(
            context: Context,
            method : Int,
            url : String,
            params : MutableMap<String, String>?,
            listener: Response.Listener<JSONObject>,
            errorListener: Response.ErrorListener?,
            authorization: Boolean = true,
            loadingScreen: Boolean = true
        ) {
            // callbacks
            var onResponse = listener
            var onError = errorListener
            // loading dialog
            if (loadingScreen) {
                val loadingDialog = Util.Companion.LoadingDialog(context as Activity)
                loadingDialog.start()
                onResponse = Response.Listener<JSONObject>
                { loadingDialog.dismiss(); listener.onResponse(it) }
                onError = Response.ErrorListener()
                { loadingDialog.dismiss(); errorListener?.onErrorResponse(it) }
            }
            // Send request
            val fullUrl = buildUrl(url, params)
            val queue = getRequestQueue(context)
            val request = object : JsonObjectRequest (
                method, fullUrl, null, onResponse, onError
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    if (!authorization) return super.getHeaders()
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer ${SessionManager.token.toString()}"
                    return headers
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                0, -1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            queue.add(request)
        }

        fun sendRequestString(
            context: Context,
            method : Int,
            url : String,
            params : MutableMap<String, String>?,
            listener: Response.Listener<String>,
            errorListener: Response.ErrorListener?,
            authorization: Boolean = true,
            loadingScreen: Boolean = true
        ) {
            // callbacks
            var onResponse = listener
            var onError = errorListener
            // loading dialog
            if (loadingScreen) {
                val loadingDialog = Util.Companion.LoadingDialog(context as Activity)
                loadingDialog.start()
                onResponse = Response.Listener<String>
                { loadingDialog.dismiss(); listener.onResponse(it) }
                onError = Response.ErrorListener()
                { loadingDialog.dismiss(); errorListener?.onErrorResponse(it) }
            }
            // Send request
            val fullUrl = buildUrl(url, params)
            val queue = getRequestQueue(context)
            val stringRequest = object : StringRequest(
                method, fullUrl, onResponse, onError
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    if (!authorization) return super.getHeaders()
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer ${SessionManager.token.toString()}"
                    return headers
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(
                0, -1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            queue.add(stringRequest)
        }

        fun sendRequestArray(
            context: Context,
            method : Int,
            url : String,
            params : MutableMap<String, String>?,
            listener: Response.Listener<JSONArray>,
            errorListener: Response.ErrorListener?,
            authorization: Boolean = true,
            loadingScreen: Boolean = true
        ) {
            // callbacks
            var onResponse = listener
            var onError = errorListener
            // loading dialog
            if (loadingScreen) {
                val loadingDialog = Util.Companion.LoadingDialog(context as Activity)
                loadingDialog.start()
                onResponse = Response.Listener<JSONArray>
                { loadingDialog.dismiss(); listener.onResponse(it) }
                onError = Response.ErrorListener()
                { loadingDialog.dismiss(); errorListener?.onErrorResponse(it) }
            }
            // Send request
            val fullUrl = buildUrl(url, params)
            val queue = getRequestQueue(context)
            val request = object : JsonArrayRequest(
                method, fullUrl, null, onResponse, onError
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    if (!authorization) return super.getHeaders()
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer ${SessionManager.token.toString()}"
                    return headers
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                0, -1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            queue.add(request)
        }

        fun sendImage(
            context: Context,
            method : Int,
            url : String,
            image : FileData,
            listener: Response.Listener<String>,
            errorListener: Response.ErrorListener?
        ) { sendImage(context, method, url, arrayListOf(image), listener, errorListener) }

        fun sendImage(
            context: Context,
            method : Int,
            url : String,
            images : ArrayList<FileData>,
            listener: Response.Listener<String>,
            errorListener: Response.ErrorListener?
        ) {
            try {
                val queue = getRequestQueue(context)

                val jsonArray = JSONArray()
                for (image in images) jsonArray.put(FileDataToJson(image))
                val requestBody = jsonArray.toString()

                val request: StringRequest = object : StringRequest(
                    method, "${BASE_API_URL}$url", listener, errorListener
                ) {
                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    @Throws(AuthFailureError::class)
                    override fun getBody(): ByteArray? {
                        return try {
                            requestBody.toByteArray(charset("utf-8"))
                        } catch (uee: UnsupportedEncodingException) {
                            VolleyLog.wtf(
                                "Unsupported Encoding while trying to get the bytes of %s using %s",
                                requestBody,
                                "utf-8"
                            )
                            null
                        }
                    }

                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Authorization"] = "Bearer ${SessionManager.token.toString()}"
                        return headers
                    }
                }
                request.retryPolicy = DefaultRetryPolicy(
                    0, -1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
                queue.add(request)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }
}