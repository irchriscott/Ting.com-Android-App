package com.codepipes.ting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.widget.TextView
import com.codepipes.ting.fragments.user.UserAbout
import com.codepipes.ting.fragments.user.UserMoments
import com.codepipes.ting.fragments.user.UserRestaurants
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class RestaurantProfile : AppCompatActivity() {

    private lateinit var mUserProfileName: TextView
    private lateinit var mUserProfileAddress: TextView
    private lateinit var mUserProfileImage: CircleImageView

    private lateinit var userAuthentication: UserAuthentication

    private lateinit var mUserTabLayout: TabLayout
    private lateinit var mUserViewPager: ViewPager
    private lateinit var mUserToolbar: Toolbar

    private lateinit var session: User
    private lateinit var branch: Branch

    @SuppressLint("SetTextI18n", "PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_profile)

        userAuthentication = UserAuthentication(this@RestaurantProfile)

        session = userAuthentication.get()!!
        branch = Gson().fromJson(intent.getStringExtra("resto"), Branch::class.java)

        mUserToolbar = findViewById<Toolbar>(R.id.userToolbar) as Toolbar
        setSupportActionBar(mUserToolbar)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = ""
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.colorPrimaryDark)))

        val upArrow = ContextCompat.getDrawable(this@RestaurantProfile, R.drawable.abc_ic_ab_back_material)
        upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantProfile, R.color.colorWhite), PorterDuff.Mode.SRC_ATOP)
        supportActionBar!!.setHomeAsUpIndicator(upArrow)

        mUserProfileName = findViewById<TextView>(R.id.userProfileName) as TextView
        mUserProfileAddress = findViewById<TextView>(R.id.userProfileAddress) as TextView
        mUserProfileImage = findViewById<CircleImageView>(R.id.userProfileImage) as CircleImageView

        mUserProfileName.text = "${branch.restaurant?.name}, ${branch.name}"
        mUserProfileAddress.text = "${branch.town}, ${branch.country}"
        Picasso.get().load(branch.restaurant?.logoURL()).into(mUserProfileImage)

        mUserTabLayout = findViewById<TabLayout>(R.id.userTabLayout) as TabLayout
        mUserViewPager = findViewById<ViewPager>(R.id.userViewPager) as ViewPager

        val adapter = UserProfile.UserProfileViewPagerAdapter(supportFragmentManager)


        mUserViewPager.adapter = adapter
        mUserTabLayout.setupWithViewPager(mUserViewPager)

        mUserViewPager.currentItem = intent.getIntExtra("tab", 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
