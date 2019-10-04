package com.codepipes.ting.fragments.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.NavigationView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.UserProfile
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class UserMenuFragment : BottomSheetDialogFragment() {

    private lateinit var mUserProfileName: TextView
    private lateinit var mUserProfileEmail: TextView
    private lateinit var mUserProfileImage: CircleImageView

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var mUserProfileMenu: NavigationView

    private lateinit var session: User

    private val mOnNavigationViewItemClickListener = NavigationView.OnNavigationItemSelectedListener {
        when(it.itemId){
            R.id.user_profile_moments -> {
                this.navigateToUserProfile(0, Gson().toJson(session))
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_restaurants -> {
                this.navigateToUserProfile(1, Gson().toJson(session))
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_profile -> {
                this.navigateToUserProfile(2, Gson().toJson(session))
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_orders -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_booking -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_settings -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_logout -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun getTheme(): Int = R.style.BaseBottomSheetDialogElse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_menu, container, false)

        userAuthentication = UserAuthentication(activity!!)
        session = userAuthentication.get()!!

        mUserProfileName = view.findViewById<TextView>(R.id.userProfileName) as TextView
        mUserProfileEmail = view.findViewById<TextView>(R.id.userProfileEmail) as TextView
        mUserProfileImage = view.findViewById<CircleImageView>(R.id.userProfileImage) as CircleImageView

        mUserProfileMenu = view.findViewById<NavigationView>(R.id.userProfileMenu) as NavigationView

        mUserProfileName.text = session.name
        mUserProfileEmail.text = session.email
        Picasso.get().load(session.imageURL()).into(mUserProfileImage)

        mUserProfileImage.setOnClickListener { this.navigateToUserProfile(0, Gson().toJson(session)) }
        mUserProfileName.setOnClickListener { this.navigateToUserProfile(0, Gson().toJson(session)) }
        mUserProfileEmail.setOnClickListener { this.navigateToUserProfile(0, Gson().toJson(session)) }

        mUserProfileMenu.setNavigationItemSelectedListener(mOnNavigationViewItemClickListener)

        return view
    }

    private fun navigateToUserProfile(tab: Int, user: String){

        val intent = Intent(activity!!, UserProfile::class.java)
        intent.putExtra("user", user)
        intent.putExtra("tab", tab)

        activity?.startActivity(intent)
    }
}
