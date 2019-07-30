package com.codepipes.ting.providers

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.codepipes.ting.models.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserAuthentication(
    private val contxt: Context
){
    private val context: Context = contxt
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()

    private val SESSION_SHARED_PREFERENCES_KEY = "session_user"

    public fun set(data: String){
        this.sharedPreferencesEditor.putString(SESSION_SHARED_PREFERENCES_KEY, data);
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    public fun get(key: String) : User?{
        return if(this.isLoggedIn()){
            val userString = this.sharedPreferences.getString(SESSION_SHARED_PREFERENCES_KEY, null)
            val gson = Gson()
            gson.fromJson(userString, User::class.java)
        } else { null }
    }

    public fun isLoggedIn(): Boolean{
        return !this.sharedPreferences.getString(SESSION_SHARED_PREFERENCES_KEY, null).isNullOrEmpty()
    }

    public fun logOut(){
        this.sharedPreferencesEditor.remove(SESSION_SHARED_PREFERENCES_KEY)
    }
}