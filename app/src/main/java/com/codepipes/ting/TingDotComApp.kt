package com.codepipes.ting

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.support.annotation.NonNull
import com.livefront.bridge.SavedStateHandler
import android.support.v4.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.support.annotation.Nullable
import android.support.multidex.MultiDex
import com.livefront.bridge.Bridge
import icepick.Icepick


class TingDotComApp () : Application() {

    override fun onCreate() {
        super.onCreate()

        Bridge.initialize(this, object : SavedStateHandler {
            override fun saveInstanceState(
                target: Any,
                state: Bundle
            ) {
                Icepick.saveInstanceState(this, state)
            }

            override fun restoreInstanceState(
                target: Any,
                @Nullable state: Bundle?
            ) {
                Icepick.restoreInstanceState(this, state)
            }
        })
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}