package com.example.skucise

import android.content.Context
import android.content.SharedPreferences

class SessionManager {
    companion object {

//        const val BASE_API_URL = "http://10.0.2.2:5000/api/"
        const val BASE_API_URL = "http://178.149.1.172:5000/api/"

        private var sharedPreferences : SharedPreferences? = null
        var currentUser: User?
            get() {
                if (sharedPreferences == null) throw Exception("ERROR :: SessionManager:: Session not loaded!!")
                val userString = sharedPreferences!!.getString("User", "Username:0")?.split(":")
                return User(id = userString!![1].toInt(), userString[0])
            }
            private set(value) {
                if (sharedPreferences == null) throw Exception("ERROR :: SessionManager:: Session not loaded!!")
                sharedPreferences!!.edit().apply {
                    val userString = "${value?.username?:"Username"}:${value?.id?:0}"
                    putString("User", userString)
                    apply()
                }
            }
        var token: String?
            get() {
                if (sharedPreferences == null) throw Exception("ERROR :: SessionManager:: Session not loaded!!")
                return sharedPreferences!!.getString("token", null)
            }
            private set(value) {
                if (sharedPreferences == null) throw Exception("ERROR :: SessionManager:: Session not loaded!!")
                sharedPreferences!!.edit().apply {
                    putString("token", value)
                    apply()
                }
            }

        fun loadSession(context: Context) {
            sharedPreferences = context.getSharedPreferences("SessionManager", 0)
        }

        fun startSession(new_token : String, user: User) {
            if (token != null) {
                throw Exception("ERROR :: Error in session :: Session already started.")
            }
            token = new_token
            currentUser = user
        }

        fun changeUsername(username: String) {
            if (currentUser != null)
                currentUser = User(id = currentUser!!.id, username = username)
        }

        fun isActive() : Boolean {
            return token == null
        }

        fun stopSession() {
            token = null
            currentUser = null
        }
    }
}