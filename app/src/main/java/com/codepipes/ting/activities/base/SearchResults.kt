package com.codepipes.ting.activities.base

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.codepipes.ting.R
import com.codepipes.ting.fragments.search.MenuSearchResults
import com.codepipes.ting.fragments.search.RestaurantSearchResults
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_search_results.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class SearchResults : AppCompatActivity() {

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        setSupportActionBar(toolbar)
        supportActionBar?.setDefaultDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        try {
            val upArrow = ContextCompat.getDrawable(this@SearchResults,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(
                ContextCompat.getColor(this@SearchResults,
                    R.color.colorPrimary
                ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        val query = intent?.getStringExtra("query")
        input_search.setText(query)

        val adapter = SearchResultsViewPagerAdapter(
            supportFragmentManager
        )
        adapter.addFragment(RestaurantSearchResults.newInstance(query?:""), "RESTAURANTS")
        adapter.addFragment(MenuSearchResults.newInstance(query?:""), "MENUS")

        result_viewpager.adapter = adapter
        result_tabs.setupWithViewPager(result_viewpager)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    internal class SearchResultsViewPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {

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
