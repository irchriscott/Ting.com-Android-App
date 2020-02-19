package com.codepipes.ting.providers

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.codepipes.ting.models.Placement
import com.google.gson.Gson

class UserPlacement (
    private val contxt: Context
){
    private val context: Context = contxt
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()

    private val PLACEMENT_SHARED_PREFERENCES_KEY    = "current_placement"
    private val PLACEMENT_TOKEN_PREFERENCES_KEY     = "current_placement_token"
    private val PLACEMENT_TEMP_TOKEN_SHARED_KEY     = "temp_placement_token"

    public fun set(data: String){
        this.sharedPreferencesEditor.putString(PLACEMENT_SHARED_PREFERENCES_KEY, data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    public fun setToken(data: String){
        this.sharedPreferencesEditor.putString(PLACEMENT_TOKEN_PREFERENCES_KEY, data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    public fun setTempToken(data: String){
        this.sharedPreferencesEditor.putString(PLACEMENT_TEMP_TOKEN_SHARED_KEY, data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    public fun get() : Placement? {
        return if(this.isPlacedIn()){
            val userString = this.sharedPreferences.getString(PLACEMENT_SHARED_PREFERENCES_KEY, null)
            val gson = Gson()
            gson.fromJson(userString, Placement::class.java)
        } else { null }
    }

    public fun getTempToken() : String? = this.sharedPreferences.getString(PLACEMENT_TEMP_TOKEN_SHARED_KEY, null)

    public fun isPlacedIn(): Boolean{
        return !this.sharedPreferences.getString(PLACEMENT_SHARED_PREFERENCES_KEY, null).isNullOrEmpty()
                || !this.sharedPreferences.getString(PLACEMENT_TOKEN_PREFERENCES_KEY, null).isNullOrEmpty()
    }

    public fun placeOut(){
        this.sharedPreferencesEditor.remove(PLACEMENT_TEMP_TOKEN_SHARED_KEY)
        this.sharedPreferencesEditor.remove(PLACEMENT_TOKEN_PREFERENCES_KEY)
        this.sharedPreferencesEditor.remove(PLACEMENT_SHARED_PREFERENCES_KEY)
    }
}