package com.codepipes.ting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.ImageButton
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.nguyenhoanglam.imagepicker.ui.imagepicker.ImagePicker
import de.hdodenhof.circleimageview.CircleImageView

class EditUserProfile : AppCompatActivity() {

    private lateinit var user: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var mProfileImageView: CircleImageView
    private lateinit var mEditProfileImageButton: ImageButton

    private val REQUEST_CODE_IMAGE_PICKER: Int = 1

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user_profile)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = resources.getString(R.string.user_profile_edit)

        val upArrow = ContextCompat.getDrawable(this@EditUserProfile, R.drawable.abc_ic_ab_back_material)
        upArrow!!.setColorFilter(ContextCompat.getColor(this@EditUserProfile, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
        supportActionBar!!.setHomeAsUpIndicator(upArrow)

        userAuthentication = UserAuthentication(this@EditUserProfile)
        user = userAuthentication.get()!!

        mProfileImageView = findViewById<CircleImageView>(R.id.edit_user_image_view) as CircleImageView
        mEditProfileImageButton = findViewById<ImageButton>(R.id.edit_user_image_button) as ImageButton

        mEditProfileImageButton.setOnClickListener {
            ImagePicker.with(this@EditUserProfile).start()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
