package com.codepipes.ting.activities.moment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.camerakit.CameraKit
import com.camerakit.CameraKitView
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.imageeditor.editimage.EditImageActivity
import com.codepipes.ting.imageeditor.editimage.ImageEditorIntentBuilder
import com.coursion.freakycoder.mediapicker.galleries.Gallery
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_capture_moment.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class CaptureMoment : AppCompatActivity() {

    private lateinit var cameraKitView: CameraKitView

    private val mProgressOverlay: ProgressOverlay =
        ProgressOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_moment)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        cameraKitView = findViewById<CameraKitView>(R.id.camera_view) as CameraKitView
        cameraKitView.facing = CameraKit.FACING_BACK
        cameraKitView.flash = CameraKit.FLASH_AUTO

        cameraKitView.errorListener = CameraKitView.ErrorListener { _, cameraException ->
            TingToast(this@CaptureMoment, cameraException.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
        }

        close_camera.setOnClickListener { onBackPressed() }
        open_gallery.setOnClickListener {
            val intent = Intent(this, Gallery::class.java)
            if (ContextCompat.checkSelfPermission(
                    this@CaptureMoment,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                intent.putExtra("title", resources.getString(R.string.edit_user_profile_select_image))
                intent.putExtra("mode", 2)
                intent.putExtra("maxSelection", 1)
                startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
            } else {
                ActivityCompat.requestPermissions(this@CaptureMoment,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_IMAGE_PICKER
                )
            }
        }
        switch_camera.setOnClickListener { cameraKitView.toggleFacing() }
        toggle_flash.setOnClickListener { toggleCameraFlash() }

        take_picture.setOnClickListener {
            cameraKitView.onPause()
            mProgressOverlay.show(supportFragmentManager, mProgressOverlay.tag)
            cameraKitView.captureImage { _, capturedImage ->
                val filename = "moment_${System.currentTimeMillis()}.jpeg"
                val path = File(Environment.getExternalStorageDirectory(), "Ting.com" + File.separator + "Moments")
                if (!path.exists()) { path.mkdirs() }
                val picture = File(path, filename)
                try {
                    val outputStream = FileOutputStream(picture)
                    outputStream.write(capturedImage)
                    outputStream.close()

                    cameraKitView.onPause()
                    mProgressOverlay.dismiss()

                    try {
                        val intent = ImageEditorIntentBuilder(this, picture.absolutePath, picture.absolutePath)
                             .withAddText()
                             .withPaintFeature()
                             .withFilterFeature()
                             .withRotateFeature()
                             .withCropFeature()
                             .withBrightnessFeature()
                             .withSaturationFeature()
                             .withBeautyFeature()
                             .withStickerFeature()
                             .forcePortrait(true)
                             .build()

                        EditImageActivity.start(this@CaptureMoment, intent, PHOTO_EDITOR_REQUEST_CODE)
                    } catch (e: Exception) { TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                } catch (e: IOException) {
                    mProgressOverlay.dismiss()
                    cameraKitView.onResume()
                    TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }
        }
    }

    private fun toggleCameraFlash() {
        if(cameraKitView.hasFlash()) {
            if(cameraKitView.flash == CameraKit.FLASH_AUTO) {
                cameraKitView.flash = CameraKit.FLASH_OFF
                toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_off))
            }
            if(cameraKitView.flash == CameraKit.FLASH_OFF) {
                cameraKitView.flash = CameraKit.FLASH_ON
                toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_on))
            }
            if(cameraKitView.flash == CameraKit.FLASH_ON) {
                cameraKitView.flash = CameraKit.FLASH_AUTO
                toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_auto))
            }
        } else {
            cameraKitView.flash = CameraKit.FLASH_OFF
            toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_off))
        }
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

    override fun onStart() {
        super.onStart()
        cameraKitView.onStart()
    }

    override fun onResume() {
        super.onResume()
        cameraKitView.onResume()
    }

    override fun onPause() {
        cameraKitView.onPause()
        super.onPause()
    }

    override fun onStop() {
        cameraKitView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

                        val filename = "moment_${System.currentTimeMillis()}.jpeg"
                        val path = File(Environment.getExternalStorageDirectory(), "Ting.com" + File.separator + "Moments")
                        if (!path.exists()) { path.mkdirs() }
                        val picture = File(path, filename)

                        try {
                            val intent = ImageEditorIntentBuilder(this, image.absolutePath, picture.absolutePath)
                                .withAddText()
                                .withPaintFeature()
                                .withFilterFeature()
                                .withRotateFeature()
                                .withCropFeature()
                                .withBrightnessFeature()
                                .withSaturationFeature()
                                .withBeautyFeature()
                                .withStickerFeature()
                                .forcePortrait(true)
                                .build()

                            EditImageActivity.start(this@CaptureMoment, intent, PHOTO_EDITOR_REQUEST_CODE)

                        } catch (e: Exception) { TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }

                    } catch (e: FileNotFoundException) {
                        TingToast(
                            this@CaptureMoment,
                            e.message!!,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                } else { TingToast(
                    this@CaptureMoment,
                    "No Image Selected",
                    TingToastType.DEFAULT
                ).showToast(Toast.LENGTH_LONG) }
            }
        } else if (requestCode == PHOTO_EDITOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val newFilePath = data.getStringExtra(ImageEditorIntentBuilder.OUTPUT_PATH)
                val isImageEdit = data.getBooleanExtra(EditImageActivity.IS_IMAGE_EDITED, false)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_PICKER: Int = 3
        private const val PHOTO_EDITOR_REQUEST_CODE: Int = 5
    }
}
