package com.codepipes.ting.activities.user

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.codepipes.ting.R
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.adapters.user.UserAddressAdapter
import com.codepipes.ting.dialogs.messages.*
import com.codepipes.ting.dialogs.user.add.AddUserAddress
import com.codepipes.ting.dialogs.user.edit.EditUserEmailAddress
import com.codepipes.ting.dialogs.user.edit.EditUserPassword
import com.codepipes.ting.interfaces.SelectItemListener
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.Constants
import com.codepipes.ting.utils.UtilsFunctions
import com.coursion.freakycoder.mediapicker.galleries.Gallery
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_edit_user_profile.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class EditUserProfile : AppCompatActivity() {

    private lateinit var user: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var mProgressOverlay: ProgressOverlay
    private lateinit var utilsFunctions: UtilsFunctions

    @SuppressLint("PrivateResource", "SetTextI18n", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user_profile)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        userAuthentication = UserAuthentication(this@EditUserProfile)
        user = userAuthentication.get()!!

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = user.username.capitalize()

        try {
            val upArrow = ContextCompat.getDrawable(this@EditUserProfile,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@EditUserProfile,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        mProgressOverlay = ProgressOverlay()
        utilsFunctions = UtilsFunctions(this@EditUserProfile)

        Picasso.get().load(user.imageURL()).into(edit_user_image_view)

        user_profile_edit_email_input.isClickable = false
        user_profile_edit_email_input.setText(user.email)
        user_profile_edit_password_input.isClickable = false
        user_profile_edit_password_input.setText("1234567")
        user_profile_edit_password_input.isEnabled = false
        user_profile_edit_password_input.keyListener = null

        edit_user_image_button.setOnClickListener {
            val intent = Intent(this, Gallery::class.java)
            if (ContextCompat.checkSelfPermission(
                    this@EditUserProfile,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                intent.putExtra("title", resources.getString(R.string.edit_user_profile_select_image))
                intent.putExtra("mode", 2)
                intent.putExtra("maxSelection", 1)
                startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
            } else {
                ActivityCompat.requestPermissions(this@EditUserProfile,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_IMAGE_GALLERY
                )
            }
        }

        user_profile_edit_email_button.setOnClickListener {
            val mEditEmailDialog = EditUserEmailAddress()
            mEditEmailDialog.show(supportFragmentManager, mEditEmailDialog.tag)
        }

        user_profile_edit_password_button.setOnClickListener {
            val mEditPasswordDialog = EditUserPassword()
            mEditPasswordDialog.show(supportFragmentManager, mEditPasswordDialog.tag)
        }

        user_profile_edit_phone_number_input.setText(user.phone)
        user_profile_edit_dob_input.setText(user.dob)

        user_profile_edit_name_input.setText(user.name)
        user_profile_edit_username_input.setText(user.username)
        user_profile_edit_gender_input.setText(user.gender)

        val genders = Constants().genders

        user_profile_edit_gender_input.setOnClickListener {
            val selectDialog = SelectDialog()
            val bundle = Bundle()
            bundle.putString(CurrentRestaurant.CONFIRM_TITLE_KEY, "Select Gender")
            selectDialog.arguments = bundle
            selectDialog.setItems(genders.toList(), object : SelectItemListener {
                override fun onSelectItem(position: Int) {
                    val selectedText = genders[position]
                    user_profile_edit_gender_input.setText(selectedText)
                    selectDialog.dismiss()
                }
            })
            selectDialog.show(supportFragmentManager, selectDialog.tag)
        }

        user_profile_edit_gender_input.setOnKeyListener(null)

        val calendar = Calendar.getInstance()
        val format = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(format, Locale.US)

        val dob = sdf.parse(user.dob)
        val dobYear =  if (dob.year > 50) { "19${dob.year}".toInt() } else { "20${dob.year}".toInt() }

        val date = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            user_profile_edit_dob_input.setText(sdf.format(calendar.time))
        }

        user_profile_edit_dob_input.setOnClickListener {
            DatePickerDialog(this@EditUserProfile,
                R.style.DatePickerAppTheme, date, dobYear, dob.month, dob.date).show()
        }
        user_profile_edit_dob_input.setOnKeyListener(null)

        user_addresses_recycle_view.layoutManager = LinearLayoutManager(this@EditUserProfile)
        user_addresses_recycle_view.adapter = UserAddressAdapter(user.addresses!!, supportFragmentManager)

        user_address_add.setOnClickListener {
            val mAddUserAddressDialog = AddUserAddress()
            mAddUserAddressDialog.show(supportFragmentManager, mAddUserAddressDialog.tag)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_profile_save_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.user_profile_save_edit -> {
                this.updateUserProfile()
                return true
            }
        }
        return false
    }

    private fun updateUserProfile(){
        val url = Routes.updateProfileIdentity
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("name", user_profile_edit_name_input.text.toString())
            .addFormDataPart("username", user_profile_edit_username_input.text.toString())
            .addFormDataPart("gender", user_profile_edit_gender_input.text.toString())
            .addFormDataPart("date_of_birth", user_profile_edit_dob_input.text.toString())
            .addFormDataPart("phone", user_profile_edit_phone_number_input.text.toString())
            .build()

        val request = Request.Builder()
            .header("Authorization", user.token!!)
            .url(url)
            .post(requestBody)
            .build()

        mProgressOverlay.show(supportFragmentManager, mProgressOverlay.tag)

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    mProgressOverlay.dismiss()
                    Picasso.get().load(user.imageURL()).into(edit_user_image_view)
                    TingToast(
                        this@EditUserProfile,
                        e.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                val gson = Gson()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    runOnUiThread {
                        mProgressOverlay.dismiss()
                        if (serverResponse.status == 200){
                            userAuthentication.set(gson.toJson(serverResponse.user))
                            TingToast(
                                this@EditUserProfile,
                                serverResponse.message,
                                TingToastType.SUCCESS
                            ).showToast(Toast.LENGTH_LONG)
                        } else { ErrorMessage(
                            this@EditUserProfile,
                            serverResponse.message
                        ).show() }
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        Picasso.get().load(user.imageURL()).into(edit_user_image_view)
                        mProgressOverlay.dismiss()
                        TingToast(
                            this@EditUserProfile,
                            "An Error Has Occurred",
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }

            }
        })
    }

    @SuppressLint("DefaultLocale")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICKER) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val selectionResult = data.getStringArrayListExtra("result")
                if(selectionResult.size > 0){
                    try {
                        val image = File(selectionResult[0])
                        val uriFromPath = Uri.fromFile(image)
                        val imageBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uriFromPath))

                        edit_user_image_view.setImageBitmap(imageBitmap)

                        val url = Routes.updateProfileImage
                        val client = OkHttpClient.Builder()
                            .connectTimeout(60, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .build()

                        val MEDIA_TYPE_PNG = "image/png".toMediaTypeOrNull()
                        val imageName = "${user.username.toLowerCase()}_${utilsFunctions.getToken(12).toLowerCase()}.png"

                        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("image", imageName, RequestBody.create(MEDIA_TYPE_PNG, image)).build()

                        val request = Request.Builder()
                            .header("Authorization", user.token!!)
                            .url(url)
                            .post(requestBody)
                            .build()

                        mProgressOverlay.show(supportFragmentManager, mProgressOverlay.tag)

                        client.newCall(request).enqueue(object : Callback {

                            override fun onFailure(call: Call, e: IOException) {
                                runOnUiThread {
                                    mProgressOverlay.dismiss()
                                    Picasso.get().load(user.imageURL()).into(edit_user_image_view)
                                    TingToast(
                                        this@EditUserProfile,
                                        e.message!!,
                                        TingToastType.ERROR
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                            }

                            override fun onResponse(call: Call, response: Response) {
                                val responseBody = response.body!!.string()
                                val gson = Gson()
                                try{
                                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                                    runOnUiThread {
                                        mProgressOverlay.dismiss()
                                        if (serverResponse.status == 200){
                                            userAuthentication.set(gson.toJson(serverResponse.user))
                                            TingToast(
                                                this@EditUserProfile,
                                                serverResponse.message,
                                                TingToastType.SUCCESS
                                            ).showToast(Toast.LENGTH_LONG)
                                        } else { TingToast(
                                            this@EditUserProfile,
                                            serverResponse.message,
                                            TingToastType.ERROR
                                        ).showToast(Toast.LENGTH_LONG) }
                                    }
                                } catch (e: Exception){
                                    runOnUiThread {
                                        Picasso.get().load(user.imageURL()).into(edit_user_image_view)
                                        mProgressOverlay.dismiss()
                                        TingToast(
                                            this@EditUserProfile,
                                            "An Error Has Occurred",
                                            TingToastType.ERROR
                                        ).showToast(Toast.LENGTH_LONG)
                                    }
                                }

                            }
                        })

                    } catch (e: FileNotFoundException) {
                        TingToast(
                            this@EditUserProfile,
                            e.message!!,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                } else { TingToast(
                    this@EditUserProfile,
                    "No Image Selected",
                    TingToastType.DEFAULT
                ).showToast(Toast.LENGTH_LONG) }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_CODE_IMAGE_GALLERY -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    val intent = Intent(this, Gallery::class.java)
                    intent.putExtra("title", resources.getString(R.string.edit_user_profile_select_image))
                    intent.putExtra("mode", 2)
                    intent.putExtra("maxSelection", 1)
                    startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        Bridge.clear(this)
        super.onBackPressed()
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

    companion object {
        private const val REQUEST_CODE_IMAGE_PICKER: Int = 1
        private const val REQUEST_CODE_IMAGE_GALLERY: Int = 5
    }
}
