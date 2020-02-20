package com.codepipes.ting

import android.annotation.SuppressLint
import android.support.v4.app.FragmentManager
import android.graphics.PorterDuff
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import com.codepipes.ting.customclasses.LockableViewPager
import com.codepipes.ting.fragments.signup.SignUpAboutFragment
import com.codepipes.ting.fragments.signup.SignUpAddressFragment
import com.codepipes.ting.fragments.signup.SignUpIdentityFragment
import com.codepipes.ting.fragments.signup.SignUpPasswordFragment


class SignUp : AppCompatActivity() {

    private lateinit var mViewPager: LockableViewPager

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = ""

        val upArrow = ContextCompat.getDrawable(this@SignUp, R.drawable.abc_ic_ab_back_material)
        upArrow!!.setColorFilter(ContextCompat.getColor(this@SignUp, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
        supportActionBar!!.setHomeAsUpIndicator(upArrow)

        mViewPager = findViewById<LockableViewPager>(R.id.pager) as LockableViewPager
        val mPagerAdapter = SignUpViewPagerAdapter(supportFragmentManager)
        mPagerAdapter.addFragment(SignUpIdentityFragment())
        mPagerAdapter.addFragment(SignUpAboutFragment())
        mPagerAdapter.addFragment(SignUpAddressFragment())
        mPagerAdapter.addFragment(SignUpPasswordFragment())
        mViewPager.adapter = mPagerAdapter
    }

    override fun onBackPressed() {
        if (mViewPager.currentItem == 0) {
            super.onBackPressed()
        } else { mViewPager.currentItem = mViewPager.currentItem - 1 }
    }


    private inner class SignUpViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm){

        private val fragments: MutableList<Fragment> = ArrayList()

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getCount(): Int = fragments.size

        public fun addFragment(fragment: Fragment){
            this.fragments.add(fragment)
        }
    }
}
