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
import com.codepipes.ting.adapters.user.UserAddressAdapter
import com.codepipes.ting.dialogs.messages.ErrorMessage
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.dialogs.user.add.AddUserAddress
import com.codepipes.ting.dialogs.user.edit.EditUserEmailAddress
import com.codepipes.ting.dialogs.user.edit.EditUserPassword
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilData
import com.codepipes.ting.utils.UtilsFunctions
import com.coursion.freakycoder.mediapicker.galleries.Gallery
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class EditUserProfile : AppCompatActivity() {

    private lateinit var user: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var mProfileImageView: CircleImageView
    private lateinit var mEditProfileImageButton: ImageButton

    private lateinit var mEditProfileEmailInput: EditText
    private lateinit var mEditProfileEmailButton: ImageButton
    private lateinit var mEditProfilePasswordInput: EditText
    private lateinit var mEditProfilePasswordButton: ImageButton

    private lateinit var mEditProfilePhoneNumberInput: EditText
    private lateinit var mEditProfileDobInput: EditText

    private lateinit var mEditProfileNameInput: EditText
    private lateinit var mEditProfileUsernameInput: EditText
    private lateinit var mEditProfileGenderInput: EditText

    private lateinit var mUserAddressesRecycleView: RecyclerView
    private lateinit var mAddUserAddressButton: Button

    private val mProgressOverlay: ProgressOverlay =
        ProgressOverlay()

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

        utilsFunctions = UtilsFunctions(this@EditUserProfile)

        mProfileImageView = findViewById<CircleImageView>(R.id.edit_user_image_view) as CircleImageView
        mEditProfileImageButton = findViewById<ImageButton>(R.id.edit_user_image_button) as ImageButton
        Picasso.get().load(user.imageURL()).into(mProfileImageView)

        mEditProfileEmailInput = findViewById<EditText>(R.id.user_profile_edit_email_input) as EditText
        mEditProfileEmailButton = findViewById<ImageButton>(R.id.user_profile_edit_email_button) as ImageButton
        mEditProfilePasswordInput = findViewById<EditText>(R.id.user_profile_edit_password_input) as EditText
        mEditProfilePasswordButton = findViewById<ImageButton>(R.id.user_profile_edit_password_button) as ImageButton

        mEditProfileEmailInput.isClickable = false
        mEditProfileEmailInput.setText(user.email)
        mEditProfilePasswordInput.isClickable = false
        mEditProfilePasswordInput.setText("1234567")
        mEditProfilePasswordInput.isEnabled = false
        mEditProfilePasswordInput.keyListener = null

        mEditProfileImageButton.setOnClickListener {
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

        mEditProfileEmailButton.setOnClickListener {
            val mEditEmailDialog = EditUserEmailAddress()
            mEditEmailDialog.show(supportFragmentManager, mEditEmailDialog.tag)
        }

        mEditProfilePasswordButton.setOnClickListener {
            val mEditPasswordDialog = EditUserPassword()
            mEditPasswordDialog.show(supportFragmentManager, mEditPasswordDialog.tag)
        }

        mEditProfilePhoneNumberInput =  findViewById<EditText>(R.id.user_profile_edit_phone_number_input) as EditText
        mEditProfileDobInput = findViewById<EditText>(R.id.user_profile_edit_dob_input) as EditText

        mEditProfilePhoneNumberInput.setText(user.phone)
        mEditProfileDobInput.setText(user.dob)

        mEditProfileNameInput = findViewById<EditText>(R.id.user_profile_edit_name_input) as EditText
        mEditProfileUsernameInput = findViewById<EditText>(R.id.user_profile_edit_username_input) as EditText
        mEditProfileGenderInput = findViewById<EditText>(R.id.user_profile_edit_gender_input) as EditText

        mEditProfileNameInput.setText(user.name)
        mEditProfileUsernameInput.setText(user.username)
        mEditProfileGenderInput.setText(user.gender)

        val genders = UtilData().genders

        mEditProfileGenderInput.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(this@EditUserProfile)
            alertDialogBuilder.setTitle("Select Gender")
            alertDialogBuilder.setItems(genders, DialogInterface.OnClickListener{ _, i ->
                val selectedText = genders[i]
                mEditProfileGenderInput.setText(selectedText) })
            val alertDialog: AlertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
        mEditProfileGenderInput.setOnKeyListener(null)

        val calendar = Calendar.getInstance()
        val format = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(format, Locale.US)

        val dob = sdf.parse(user.dob)
        val dobYear =  if (dob.year > 50) { "19${dob.year}".toInt() } else { "20${dob.year}".toInt() }

        val date = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            mEditProfileDobInput.setText(sdf.format(calendar.time))
        }

        mEditProfileDobInput.setOnClickListener {
            DatePickerDialog(this@EditUserProfile,
                R.style.DatePickerAppTheme, date, dobYear, dob.month, dob.date).show()
        }
        mEditProfileDobInput.setOnKeyListener(null)

        mUserAddressesRecycleView = findViewById<RecyclerView>(R.id.user_addresses_recycle_view) as RecyclerView
        mUserAddressesRecycleView.layoutManager = LinearLayoutManager(this@EditUserProfile)
        mUserAddressesRecycleView.adapter = UserAddressAdapter(user.addresses!!, supportFragmentManager)

        mAddUserAddressButton = findViewById<Button>(R.id.user_address_add) as Button
        mAddUserAddressButton.setOnClickListener {
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
            .addFormDataPart("name", mEditProfileNameInput.text.toString())
            .addFormDataPart("username", mEditProfileUsernameInput.text.toString())
            .addFormDataPart("gender", mEditProfileGenderInput.text.toString())
            .addFormDataPart("date_of_birth", mEditProfileDobInput.text.toString())
            .addFormDataPart("phone", mEditProfilePhoneNumberInput.text.toString())
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
                    Picasso.get().load(user.imageURL()).into(mProfileImageView)
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
                        Picasso.get().load(user.imageURL()).into(mProfileImageView)
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

                        mProfileImageView.setImageBitmap(imageBitmap)

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
                                    Picasso.get().load(user.imageURL()).into(mProfileImageView)
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
                                        Picasso.get().load(user.imageURL()).into(mProfileImageView)
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
