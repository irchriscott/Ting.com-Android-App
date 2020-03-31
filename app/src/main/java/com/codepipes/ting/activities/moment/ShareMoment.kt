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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codepipes.ting.R
import com.codepipes.ting.services.UploadMomentService
import com.facebook.FacebookSdk
import com.facebook.share.model.*
import com.facebook.share.widget.ShareDialog
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_share_moment.*
import java.io.File


class ShareMoment : AppCompatActivity() {

    private var usePackage: Boolean = true

    private var fileType = 1
    private var filePath = ""

    @SuppressLint("PrivateResource", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_moment)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        FacebookSdk.sdkInitialize(this@ShareMoment)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Share Moment".toUpperCase()

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
            Picasso.get().load(File(filePath)).into(moment_image)
        } else {
            moment_video.visibility = View.VISIBLE
            moment_image.visibility = View.GONE
            moment_video.setSource(filePath)
        }
    }

    private fun shareWithFacebookFeed() {

        if(fileType == 1) {
            val image = BitmapFactory.decodeFile(filePath)

            val sharePhoto = SharePhoto.Builder()
                .setBitmap(image)
                .build()

            val sharePhotoContent = SharePhotoContent.Builder()
                .addPhoto(sharePhoto)
                .setContentUrl(Uri.parse(attributionLinkUrl))
                .setShareHashtag(ShareHashtag.Builder()
                    .setHashtag("#TingDotCom #RestoMoment")
                    .build())
                .build()

            ShareDialog.show(this@ShareMoment, sharePhotoContent)

        } else {

            val videoUri = Uri.fromFile(File(filePath!!))
            val shareVideo = ShareVideo.Builder()
                .setLocalUrl(videoUri)
                .build()

            val shareVideoContent = ShareVideoContent.Builder()
                .setVideo(shareVideo)
                .setContentUrl(Uri.parse(attributionLinkUrl))
                .setShareHashtag(ShareHashtag.Builder()
                    .setHashtag("#TingDotCom #RestoMoment")
                    .build())
                .setContentDescription(moment_caption.text.toString())
                .build()

            ShareDialog.show(this@ShareMoment, shareVideoContent)
        }
    }

    private fun shareWithFacebookStory() {

        if(usePackage) {

            if (fileType == 1) {
                val image = BitmapFactory.decodeFile(filePath)

                val sharePhoto = SharePhoto.Builder()
                    .setBitmap(image)
                    .build()

                val sharePhotoContent = ShareStoryContent.Builder()
                    .setBackgroundAsset(sharePhoto)
                    .setContentUrl(Uri.parse(attributionLinkUrl))
                    .setShareHashtag(
                        ShareHashtag.Builder()
                            .setHashtag("#TingDotCom #RestoMoment")
                            .build()
                    )
                    .build()

                ShareDialog.show(this@ShareMoment, sharePhotoContent)

            } else {

                val videoUri = Uri.fromFile(File(filePath!!))
                val shareVideo = ShareVideo.Builder()
                    .setLocalUrl(videoUri)
                    .build()

                val shareVideoContent = ShareStoryContent.Builder()
                    .setBackgroundAsset(shareVideo)
                    .setContentUrl(Uri.parse(attributionLinkUrl))
                    .setShareHashtag(
                        ShareHashtag.Builder()
                            .setHashtag("#TingDotCom #RestoMoment")
                            .build()
                    )
                    .build()

                ShareDialog.show(this@ShareMoment, shareVideoContent)
            }
        } else {

            val backgroundAssetUri = Uri.fromFile(File(filePath))
            val attributionLinkUrl = "https://ting.com/wb/usr/g/moments/"

            val intent = Intent("com.instagram.share.ADD_TO_STORY")

            intent.type = MEDIA_TYPE_JPEG
            intent.putExtra("com.facebook.platform.extra.APPLICATION_ID", resources.getString(R.string.facebook_app_id))
            intent.setDataAndType(backgroundAssetUri, MEDIA_TYPE_JPEG)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.putExtra("content_url", attributionLinkUrl)
            intent.putExtra("top_background_color", "#908CED")
            intent.putExtra("bottom_background_color", "#B56FE8")

            if (packageManager.resolveActivity(intent, 0) != null) { startActivityForResult(intent, 0) }
        }
    }

    private fun shareWithInstagramFeed() {

        val type = if(fileType == 1) { "image/*" } else { "video/*" }

        val share = Intent(Intent.ACTION_SEND)
        share.type = type

        val uri = Uri.fromFile(File(filePath!!))
        share.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(share, "Share to"))
    }

    private fun shareWithInstagramStory() {

        val backgroundAssetUri = Uri.fromFile(File(filePath!!))

        val intent = Intent("com.instagram.share.ADD_TO_STORY")

        intent.setDataAndType(backgroundAssetUri, MEDIA_TYPE_JPEG)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.putExtra("content_url", attributionLinkUrl)
        intent.putExtra("top_background_color", "#908CED")
        intent.putExtra("bottom_background_color", "#B56FE8")

        if (packageManager.resolveActivity(intent, 0) != null) { startActivityForResult(intent, 0) }
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
                //startService(uploadMomentService)

                shareWithFacebookFeed()
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
        private const val MEDIA_TYPE_JPEG = "image/jpeg"
        private const val attributionLinkUrl = "https://ting.com/wb/usr/g/moments/"

        public const val MOMENT_FILE_PATH_KEY = "file_path"
        public const val MOMENT_CONTENT_TEXT_KEY = "content"
        public const val MOMENT_FILE_TYPE_KEY = "file_type"
    }
}
