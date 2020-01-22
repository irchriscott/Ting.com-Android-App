package com.codepipes.ting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import com.codepipes.ting.fragments.cuisine.CuisineMenusFragment
import com.codepipes.ting.fragments.cuisine.CuisineRestaurantsFragment
import com.codepipes.ting.models.RestaurantCategory
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_cuisine.*

class Cuisine : AppCompatActivity() {

    @SuppressLint("PrivateResource", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cuisine)

        val cuisineString = intent.getStringExtra("cuisine")
        val cuisine = Gson().fromJson(cuisineString, RestaurantCategory::class.java)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = cuisine.name.toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@Cuisine, R.drawable.abc_ic_ab_back_material)
            upArrow!!.setColorFilter(ContextCompat.getColor(this@Cuisine, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        val adapter = CuisineViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(CuisineRestaurantsFragment.newInstance(Gson().toJson(cuisine)), "RESTAURANTS")
        adapter.addFragment(CuisineMenusFragment.newInstance(Gson().toJson(cuisine)), "MENUS")

        cuisines_viewpager.adapter = adapter
        cuisines_tabs.setupWithViewPager(cuisines_viewpager)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    internal class CuisineViewPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {

        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentsTitle: MutableList<String> = ArrayList()

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getCount(): Int = fragments.size

        override fun getPageTitle(position: Int): CharSequence? = fragmentsTitle[position]

        public fun addFragment(fragment: Fragment, title: String){
            this.fragments.add(fragment)
            this.fragmentsTitle.add(title)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
    }
}
