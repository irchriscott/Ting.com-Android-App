package com.codepipes.ting.fragments.navigation

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.activities.user.UserProfile
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.google.android.material.navigation.NavigationView
import com.livefront.bridge.Bridge
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
                this.navigateToUserProfile(0, session.id, session.urls.apiGet, session.urls.apiGetAuth)
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_restaurants -> {
                this.navigateToUserProfile(1, session.id, session.urls.apiGet, session.urls.apiGetAuth)
                return@OnNavigationItemSelectedListener true
            }
            R.id.user_profile_profile -> {
                this.navigateToUserProfile(2, session.id, session.urls.apiGet, session.urls.apiGetAuth)
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
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
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
        Picasso.get().load(session.imageURL()).fit().into(mUserProfileImage)

        mUserProfileImage.setOnClickListener { this.navigateToUserProfile(0, session.id, session.urls.apiGet, session.urls.apiGetAuth) }
        mUserProfileName.setOnClickListener { this.navigateToUserProfile(0, session.id, session.urls.apiGet, session.urls.apiGetAuth) }
        mUserProfileEmail.setOnClickListener { this.navigateToUserProfile(0, session.id, session.urls.apiGet, session.urls.apiGetAuth) }

        mUserProfileMenu.elevation = 0.0f
        mUserProfileMenu.setNavigationItemSelectedListener(mOnNavigationViewItemClickListener)

        return view
    }

    private fun navigateToUserProfile(tab: Int, id: Int, url: String, authUrl: String){

        val intent = Intent(activity!!, UserProfile::class.java)
        intent.putExtra("user", id)
        intent.putExtra("url", url)
        intent.putExtra("authUrl", authUrl)
        intent.putExtra("tab", tab)

        activity?.startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
    }
}
