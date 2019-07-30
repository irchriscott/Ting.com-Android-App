package com.codepipes.ting.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Settings(
    private val contxt: Context
) {
    private val context: Context = contxt
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()

    public fun saveSettingToSharedPreferences(key: String, data: String){
        this.sharedPreferencesEditor.putString(key, data);
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    public fun getSettingFromSharedPreferences(key: String) : String?{
        return this.sharedPreferences.getString(key, null)
    }

    public fun removeSettingFromSharedPreferences(key: String){
        this.sharedPreferencesEditor.remove(key)
    }
}



