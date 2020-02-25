package com.codepipes.ting.activities.base

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.codepipes.ting.R
import com.codepipes.ting.providers.UserAuthentication



class SplashScreen : AppCompatActivity() {

    lateinit var mAppNameText: TextView
    lateinit var mAnimation: Animation

    lateinit var userAuthentication: UserAuthentication

    private var handler: Handler? = null
    private val runnable: Runnable = Runnable {
        if(userAuthentication.isLoggedIn()){
            startActivity(Intent(this@SplashScreen, TingDotCom::class.java))
        } else {
            startActivity(Intent(this@SplashScreen, LogIn::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        userAuthentication = UserAuthentication(this@SplashScreen)

        mAppNameText = findViewById<TextView>(R.id.appNameText) as TextView

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        mAnimation = AnimationUtils.loadAnimation(this@SplashScreen,
            R.anim.fade_in
        )
        mAppNameText.startAnimation(mAnimation)

        handler = Handler()
        handler?.postDelayed(runnable, 3000)


    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacks(runnable)
    }
}
