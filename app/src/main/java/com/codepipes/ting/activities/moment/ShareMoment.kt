package com.codepipes.ting.activities.moment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.services.UploadMomentService
import com.facebook.share.model.*
import com.facebook.share.widget.ShareDialog
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_share_moment.*
import java.io.File


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ShareMoment : AppCompatActivity() {

    private var usePackage: Boolean = true

    private var fileType = 1
    private var filePath = ""

    private lateinit var userPlacement: UserPlacement

    @SuppressLint("PrivateResource", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_moment)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Share Moment".toUpperCase()

        userPlacement = UserPlacement(this@ShareMoment)

        try {
            val upArrow = ContextCompat.getDrawable(this@ShareMoment,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(
                ContextCompat.getColor(this@ShareMoment,
                    R.color.colorPrimary
                ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        fileType = intent.getIntExtra(CaptureMoment.SHARE_MOMENT_FILE_TYPE, 1)
        filePath = intent.getStringExtra(CaptureMoment.SHARE_MOMENT_FILE_PATH)?:""

        if(fileType == 1) {
            moment_video.visibility = View.GONE
            moment_image.visibility = View.VISIBLE
            Picasso.get().load(File(filePath)).fit().into(moment_image)
        } else {
            moment_video.visibility = View.VISIBLE
            moment_image.visibility = View.GONE
            moment_video.setSource(filePath)
        }

        val message = "At ${userPlacement.get()?.table?.branch?.restaurant?.name}, ${userPlacement.get()?.table?.branch?.name}"
        moment_caption.setText(message)

        share_facebook_feed.setOnCheckedChangeListener { _, isChecked -> if(isChecked) { shareWithFacebookFeed() } }
        share_facebook_story.setOnCheckedChangeListener { _, isChecked -> if(isChecked) { shareWithFacebookStory() } }
        share_instagram_story.setOnCheckedChangeListener { _, isChecked -> if(isChecked) { shareWithInstagramStory() } }
        share_others.setOnCheckedChangeListener { _, isChecked -> if(isChecked) { shareWithOtherApps() } }
    }

    private fun shareWithFacebookFeed() {

        val message = "At ${userPlacement.get()?.table?.branch?.restaurant?.name}, ${userPlacement.get()?.table?.branch?.name}"

        if(fileType == 1) {
            val image = BitmapFactory.decodeFile(filePath)

            val sharePhoto = SharePhoto.Builder()
                .setBitmap(image)
                .build()

            val sharePhotoContent = SharePhotoContent.Builder()
                .addPhoto(sharePhoto)
                .setShareHashtag(ShareHashtag.Builder()
                    .setHashtag("#TingDotCom #RestoMoment")
                    .build())
                .build()

            ShareDialog.show(this@ShareMoment, sharePhotoContent)

        } else {

            val videoUri = Uri.fromFile(File(filePath))
            val shareVideo = ShareVideo.Builder()
                .setLocalUrl(videoUri)
                .build()

            val shareVideoContent = ShareVideoContent.Builder()
                .setVideo(shareVideo)
                .setShareHashtag(ShareHashtag.Builder()
                    .setHashtag("#TingDotCom #RestoMoment")
                    .build())
                .setContentDescription(moment_caption.text.toString())
                .build()

            ShareDialog.show(this@ShareMoment, shareVideoContent)
        }
    }

    private fun shareWithFacebookStory() {

        if (fileType == 1) {
            val image = BitmapFactory.decodeFile(filePath)

            val sharePhoto = SharePhoto.Builder()
                .setBitmap(image)
                .build()

            val sharePhotoContent = ShareStoryContent.Builder()
                .setBackgroundAsset(sharePhoto)
                .setContentUrl(Uri.parse("https://www.facebook.com/irchriscott/"))
                .setBackgroundColorList(mutableListOf<String>("#908CED", "#B56FE8"))
                .setShareHashtag(
                    ShareHashtag.Builder()
                        .setHashtag("#TingDotCom #RestoMoment")
                        .build()
                )
                .build()

            ShareDialog.show(this@ShareMoment, sharePhotoContent)

        } else {

            val videoUri = Uri.fromFile(File(filePath))
            val shareVideo = ShareVideo.Builder()
                .setLocalUrl(videoUri)
                .build()

            val shareVideoContent = ShareStoryContent.Builder()
                .setBackgroundAsset(shareVideo)
                .setContentUrl(Uri.parse("https://www.facebook.com/irchriscott/"))
                .setBackgroundColorList(mutableListOf<String>("#908CED", "#B56FE8"))
                .setShareHashtag(
                    ShareHashtag.Builder()
                        .setHashtag("#TingDotCom #RestoMoment")
                        .build()
                )
                .build()

            ShareDialog.show(this@ShareMoment, shareVideoContent)
        }
    }

    private fun shareWithOtherApps() {

        val type = if(fileType == 1) { "image/*" } else { "video/*" }
        val message = "At ${userPlacement.get()?.table?.branch?.restaurant?.name}, ${userPlacement.get()?.table?.branch?.name} #TingDotCom #RestoMoment"

        val share = Intent(Intent.ACTION_SEND)
        share.type = type

        val uri = Uri.fromFile(File(filePath))
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.putExtra(Intent.EXTRA_TEXT, message)
        share.putExtra(Intent.EXTRA_TITLE, message)
        share.putExtra(Intent.EXTRA_SUBJECT, message)
        startActivity(Intent.createChooser(share, "Share to"))
    }

    private fun shareWithInstagramStory() {

        val backgroundAssetUri = Uri.fromFile(File(filePath))

        val intent = Intent("com.instagram.share.ADD_TO_STORY")

        intent.setDataAndType(backgroundAssetUri, MEDIA_TYPE_JPEG)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.putExtra("content_url", "https://www.instagram.com/ir_chris_scott/")
        intent.putExtra("top_background_color", "#908CED")
        intent.putExtra("bottom_background_color", "#B56FE8")

        if (packageManager.resolveActivity(intent, 0) != null) { startActivityForResult(intent, 0) }
        else { TingToast(this@ShareMoment, "Please, Install Instagram to proceed", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.moment_share_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.share_moment -> {

                val uploadMomentService = Intent(applicationContext, UploadMomentService::class.java)
                uploadMomentService.putExtra(MOMENT_FILE_TYPE_KEY, fileType)
                uploadMomentService.putExtra(MOMENT_FILE_PATH_KEY, filePath)
                uploadMomentService.putExtra(MOMENT_CONTENT_TEXT_KEY, moment_caption.text.toString())
                startService(uploadMomentService)
                TingToast(this@ShareMoment, "Posting Moment...", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
                onBackPressed()
            }
        }
        return false
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
        private const val MEDIA_TYPE_JPEG           = "image/jpeg"
        private const val attributionLinkUrl        = "https://ting.com/wb/usr/g/moments/"

        public const val MOMENT_FILE_PATH_KEY       = "file_path"
        public const val MOMENT_CONTENT_TEXT_KEY    = "content"
        public const val MOMENT_FILE_TYPE_KEY       = "file_type"
    }
}
