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
import android.view.View
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.utils.Routes
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_user_profile.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.TimeUnit


class UserProfile : AppCompatActivity() {

    private lateinit var mUserProfileName: TextView
    private lateinit var mUserProfileAddress: TextView
    private lateinit var mUserProfileImage: CircleImageView

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var localData: LocalData

    private lateinit var mUserTabLayout: TabLayout
    private lateinit var mUserViewPager: ViewPager
    private lateinit var mUserToolbar: Toolbar

    private lateinit var session: User
    private lateinit var user: User

    private var userId: Int = 0

    @SuppressLint("PrivateResource", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        userAuthentication = UserAuthentication(this@UserProfile)
        session = userAuthentication.get()!!

        localData = LocalData(this@UserProfile)

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

        userId = intent.getIntExtra("user", 0)
        val url = intent.getStringExtra("url")
        val authUrl = intent.getStringExtra("authUrl")

        if(localData.getUser(userId) != null){
            user = localData.getUser(userId)!!
            this.setupUser(user)

            if(userId == session.id){
                this.loadUser("${Routes().HOST_END_POINT}${authUrl}", false, session.token!!)
            } else { this.loadUser("${Routes().HOST_END_POINT}${url}", false, session.token!!) }

        } else {
            shimmerLoader.startShimmer()
            shimmerLoader.visibility = View.VISIBLE
            userProfileData.visibility = View.GONE

            if(userId == session.id){
                this.loadUser("${Routes().HOST_END_POINT}${authUrl}", true, session.token!!)
            } else { this.loadUser("${Routes().HOST_END_POINT}${url}", true, session.token!!) }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUser(user: User){

        shimmerLoader.stopShimmer()
        shimmerLoader.visibility = View.GONE

        userProfileData.visibility = View.VISIBLE

        mUserProfileName.text = user.name
        mUserProfileAddress.text = "${user.town}, ${user.country}"
        Picasso.get().load(user.imageURL()).into(mUserProfileImage)

        mUserTabLayout = findViewById<TabLayout>(R.id.userTabLayout) as TabLayout
        mUserViewPager = findViewById<ViewPager>(R.id.userViewPager) as ViewPager

        val adapter = UserProfileViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(UserMoments.newInstance(Gson().toJson(user)), resources.getString(R.string.user_profile_moments))
        adapter.addFragment(UserRestaurants.newInstance(Gson().toJson(user)), resources.getString(R.string.user_profile_restaurants))
        adapter.addFragment(UserAbout.newInstance(Gson().toJson(user)), resources.getString(R.string.user_profile_profile))

        mUserViewPager.adapter = adapter
        mUserTabLayout.setupWithViewPager(mUserViewPager)

        mUserViewPager.currentItem = intent.getIntExtra("tab", 0)
    }

    @SuppressLint("NewApi", "DefaultLocale")
    private fun loadUser(url: String, load: Boolean, token: String){
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder()
            .header("Authorization", token)
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(this@UserProfile, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                try{
                    user = Gson().fromJson(dataString, User::class.java)
                    runOnUiThread {
                        localData.updateUser(user)
                        if(load){ setupUser(user) }
                    }
                } catch (e: Exception){}
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(userId == session.id){ menuInflater.inflate(R.menu.user_profile_menu_edit, menu) }
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
