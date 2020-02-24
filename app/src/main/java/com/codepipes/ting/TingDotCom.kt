package com.codepipes.ting

import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.androidstudy.networkmanager.Monitor
import com.androidstudy.networkmanager.Tovuti
import com.codepipes.ting.fragments.navigation.DiscoveryFragment
import com.codepipes.ting.fragments.navigation.MomentsFragment
import com.codepipes.ting.fragments.navigation.RestaurantsFragment
import com.codepipes.ting.fragments.navigation.UserMenuFragment
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.services.PubnubService
import com.codepipes.ting.services.PushNotificationService
import com.livefront.bridge.Bridge
import com.pusher.pushnotifications.PushNotifications
import kotlin.system.exitProcess


class TingDotCom : AppCompatActivity() {

    private lateinit var mAppNameText: TextView
    private lateinit var mToolbar: Toolbar
    private lateinit var mNavigation: BottomNavigationView

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var snackbar: Snackbar

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->

        when (item.itemId) {
            R.id.discovery_item_menu -> {
                changeFragment(DiscoveryFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.restaurants_item_menu -> {
                changeFragment(RestaurantsFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_menu_item_menu -> {
                val mUserMenu = UserMenuFragment()
                mUserMenu.show(supportFragmentManager, mUserMenu.tag)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ting_dot_com)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        mAppNameText = findViewById<TextView>(R.id.appNameText) as TextView
        mToolbar = findViewById<Toolbar>(R.id.toolbar) as Toolbar
        mNavigation = findViewById<BottomNavigationView>(R.id.navigation) as BottomNavigationView

        mToolbar.inflateMenu(R.menu.toolbar_menu)
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        changeFragment(DiscoveryFragment())

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        userAuthentication = UserAuthentication(this@TingDotCom)
        val session = userAuthentication.get()!!

        startService(Intent(applicationContext, PushNotificationService::class.java))
        startService(Intent(applicationContext, PubnubService::class.java))

        PushNotifications.start(applicationContext, "f47c28dd-63ae-49c0-9f30-88560b21e061")
        PushNotifications.addDeviceInterest(session.channel)

        snackbar = Snackbar.make(findViewById<View>(android.R.id.content), "Checking Connectivity...", Snackbar.LENGTH_INDEFINITE)
        snackbar.show()

        Tovuti.from(this).monitor { _, isConnected, isFast ->
            runOnUiThread {
                if(isConnected) {
                    if(isFast) {
                        snackbar.setText("Connected !")
                        snackbar.view.setBackgroundColor(resources.getColor(R.color.colorGreen))
                    } else {
                        snackbar.setText("Slow Connection !")
                        snackbar.view.setBackgroundColor(resources.getColor(R.color.colorOrange))
                    }
                    snackbar.duration = Snackbar.LENGTH_SHORT
                    snackbar.show()
                } else {
                    snackbar.setText("No Internet Connection !")
                    snackbar.view.setBackgroundColor(resources.getColor(R.color.colorRed))
                    snackbar.duration = Snackbar.LENGTH_SHORT
                    snackbar.show()
                }
            }
        }
    }

    private fun changeFragment(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.navigation_fragment, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { outPersistentState?.clear() }
    }

    override fun onPause() {
        Tovuti.from(this).stop()
        snackbar.dismiss()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
        finishAffinity()
        Tovuti.from(this).stop()
        snackbar.dismiss()
        exitProcess(0)
    }
}