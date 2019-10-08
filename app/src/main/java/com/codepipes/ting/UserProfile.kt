package com.codepipes.ting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.codepipes.ting.fragments.user.UserAbout
import com.codepipes.ting.fragments.user.UserMoments
import com.codepipes.ting.fragments.user.UserRestaurants
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import com.livefront.bridge.Bridge


class UserProfile : AppCompatActivity() {

    private lateinit var mUserProfileName: TextView
    private lateinit var mUserProfileAddress: TextView
    private lateinit var mUserProfileImage: CircleImageView

    private lateinit var userAuthentication: UserAuthentication

    private lateinit var mUserTabLayout: TabLayout
    private lateinit var mUserViewPager: ViewPager
    private lateinit var mUserToolbar: Toolbar

    private lateinit var session: User
    private lateinit var user: User

    @SuppressLint("PrivateResource", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        userAuthentication = UserAuthentication(this@UserProfile)

        session = userAuthentication.get()!!
        user = Gson().fromJson(intent.getStringExtra("user"), User::class.java)

        mUserToolbar = findViewById<Toolbar>(R.id.userToolbar) as Toolbar
        setSupportActionBar(mUserToolbar)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = ""
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.colorPrimaryDark)))

        val upArrow = ContextCompat.getDrawable(this@UserProfile, R.drawable.abc_ic_ab_back_material)
        upArrow!!.setColorFilter(ContextCompat.getColor(this@UserProfile, R.color.colorWhite), PorterDuff.Mode.SRC_ATOP)
        supportActionBar!!.setHomeAsUpIndicator(upArrow)

        mUserProfileName = findViewById<TextView>(R.id.userProfileName) as TextView
        mUserProfileAddress = findViewById<TextView>(R.id.userProfileAddress) as TextView
        mUserProfileImage = findViewById<CircleImageView>(R.id.userProfileImage) as CircleImageView

        mUserProfileName.text = user.name
        mUserProfileAddress.text = "${user.town}, ${user.country}"
        Picasso.get().load(user.imageURL()).into(mUserProfileImage)

        mUserTabLayout = findViewById<TabLayout>(R.id.userTabLayout) as TabLayout
        mUserViewPager = findViewById<ViewPager>(R.id.userViewPager) as ViewPager

        val adapter = UserProfileViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(UserMoments(), resources.getString(R.string.user_profile_moments))
        adapter.addFragment(UserRestaurants(), resources.getString(R.string.user_profile_restaurants))
        adapter.addFragment(UserAbout(), resources.getString(R.string.user_profile_profile))

        mUserViewPager.adapter = adapter
        mUserTabLayout.setupWithViewPager(mUserViewPager)

        mUserViewPager.currentItem = intent.getIntExtra("tab", 0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(user.id == session.id){ menuInflater.inflate(R.menu.user_profile_menu_edit, menu) }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.user_profile_edit -> {
                if(user.id == session.id){ startActivity(Intent(this@UserProfile, EditUserProfile::class.java)) }
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    internal class UserProfileViewPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {

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
