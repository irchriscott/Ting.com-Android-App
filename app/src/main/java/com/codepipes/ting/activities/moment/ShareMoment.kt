package com.codepipes.ting.activities.moment

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import com.codepipes.ting.R
import com.facebook.share.model.*
import com.facebook.share.widget.ShareDialog
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_share_moment.*
import java.io.File
import java.lang.Exception

class ShareMoment : AppCompatActivity() {

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

        val fileType = intent.getIntExtra(CaptureMoment.SHARE_MOMENT_FILE_TYPE, 1)
        val filePath = intent.getStringExtra(CaptureMoment.SHARE_MOMENT_FILE_PATH)
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
        val fileType = intent.getIntExtra(CaptureMoment.SHARE_MOMENT_FILE_TYPE, 1)
        val filePath = intent.getStringExtra(CaptureMoment.SHARE_MOMENT_FILE_PATH)

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

            val shareDialog = ShareDialog(this@ShareMoment)
            shareDialog.show(sharePhotoContent, ShareDialog.Mode.AUTOMATIC)

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

            val shareDialog = ShareDialog(this@ShareMoment)
            shareDialog.show(shareVideoContent, ShareDialog.Mode.AUTOMATIC)
        }
    }

    private fun shareWithFacebookStory() {

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
}
