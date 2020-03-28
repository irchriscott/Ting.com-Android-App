package com.codepipes.ting.activities.moment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.util.Log
import android.view.ContextMenu
import android.view.View
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
import com.iammert.library.cameravideobuttonlib.CameraVideoButton
import com.livefront.bridge.Bridge
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.size.AspectRatio
import com.otaliastudios.cameraview.size.Size
import com.otaliastudios.cameraview.size.SizeSelectors
import kotlinx.android.synthetic.main.activity_capture_moment.*
import kotlinx.android.synthetic.main.simpledialogfragment_image.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class CaptureMoment : AppCompatActivity() {

    private lateinit var cameraView: CameraView
    private lateinit var cameraKitView: CameraKitView

    private var isUsingCameraKit = false

    private val mProgressOverlay: ProgressOverlay =
        ProgressOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_moment)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        if(isUsingCameraKit) { initializeCameraViewKit() }
        else { initializeCameraView() }

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
                    REQUEST_CODE_IMAGE_GALLERY
                )
            }
        }

        if (ContextCompat.checkSelfPermission(
                this@CaptureMoment,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this@CaptureMoment,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_RECORD_AUDIO
            )
        }

        switch_camera.setOnClickListener {
            if(isUsingCameraKit){ try { cameraKitView.toggleFacing() } catch (e: Exception) {} }
            else {
                when(cameraView.facing) {
                    Facing.BACK -> { cameraView.facing = Facing.FRONT }
                    Facing.FRONT -> { cameraView.facing = Facing.BACK }
                }
            }
        }

        toggle_flash.setOnClickListener { toggleCameraFlash() }

        take_picture.setVideoDuration(30000)
        take_picture.enableVideoRecording(!isUsingCameraKit)
        take_picture.enablePhotoTaking(true)

        take_picture.actionListener = object : CameraVideoButton.ActionListener {
            override fun onDurationTooShortError() {}

            override fun onEndRecord() {
                if(isUsingCameraKit) {
                    cameraKitView.startVideo()
                } else {
                    cameraView.apply {
                        stopVideo()
                        open()
                    }
                }
            }

            override fun onSingleTap() {
                mProgressOverlay.show(supportFragmentManager, mProgressOverlay.tag)
                if(isUsingCameraKit){
                    cameraKitView.captureImage { _, data ->  saveCapturedPicture(data) }
                } else {
                    cameraView.apply {
                        mode = Mode.PICTURE
                        takePicture()
                    }
                }
            }

            override fun onStartRecord() {
                if(isUsingCameraKit) {
                    cameraKitView.captureVideo { _, _ ->  }
                } else {
                    cameraView.mode = Mode.VIDEO

                    val videoFilename = "moment_video_${System.currentTimeMillis()}.mp4"
                    val videoPath = File(Environment.getExternalStorageDirectory(), "Ting.com" + File.separator + "Moments" + File.separator + "Videos")
                    if (!videoPath.exists()) { videoPath.mkdirs() }
                    val video = File(videoPath, videoFilename)

                    cameraView.takeVideo(video)
                }
            }
        }
    }

    private fun toggleCameraFlash() {

        if(isUsingCameraKit) {
            if(cameraKitView.hasFlash()) {
                when (cameraKitView.flash) {
                    CameraKit.FLASH_AUTO -> {
                        cameraKitView.flash = CameraKit.FLASH_OFF
                        toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_off))
                    }
                    CameraKit.FLASH_OFF -> {
                        cameraKitView.flash = CameraKit.FLASH_ON
                        toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_on))
                    }
                    CameraKit.FLASH_ON -> {
                        cameraKitView.flash = CameraKit.FLASH_AUTO
                        toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_auto))
                    }
                }
            } else {
                cameraKitView.flash = CameraKit.FLASH_OFF
                toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_off))
            }
        } else {
            when (cameraView.flash) {
                Flash.AUTO -> {
                    cameraView.flash = Flash.OFF
                    toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_off))
                }
                Flash.OFF -> {
                    cameraView.flash = Flash.ON
                    toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_on))
                }
                Flash.ON -> {
                    cameraView.flash = Flash.AUTO
                    toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_auto))
                }
                else -> {
                    cameraView.flash = Flash.TORCH
                    toggle_flash.setImageDrawable(resources.getDrawable(R.drawable.ic_camera_flash_on))
                }
            }
        }
    }

    private fun initializeCameraView() {
        camera_view_kit.visibility = View.GONE
        camera_view.visibility = View.VISIBLE

        cameraView = findViewById<CameraView>(R.id.camera_view) as CameraView
        cameraView.videoMaxDuration = 30000
        cameraView.mode = Mode.PICTURE
        cameraView.facing = Facing.BACK
        cameraView.flash = Flash.AUTO

        val display = windowManager.defaultDisplay
        val size = Point()
        display. getRealSize(size)

        val width = SizeSelectors.minWidth(size.x)
        val height = SizeSelectors.minHeight(size.y)
        val dimensions = SizeSelectors.and(width, height)
        val ratio = SizeSelectors.aspectRatio(AspectRatio.of(1, 1), 0.0f)

        val result = SizeSelectors.or(
            SizeSelectors.and(ratio, dimensions),
            ratio,
            SizeSelectors.biggest()
        )

        cameraView.setPictureSize(result)
        cameraView.setVideoSize(result)

        cameraView.addCameraListener(object : CameraListener() {
            override fun onCameraOpened(options: CameraOptions) {}
            override fun onCameraClosed() {}
            override fun onCameraError(error: CameraException) { TingToast(this@CaptureMoment, error.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
            override fun onPictureTaken(result: PictureResult) { saveCapturedPicture(result.data) }
            override fun onVideoTaken(result: VideoResult) {
                cameraView.mode = Mode.PICTURE
                try {
                    val videoFile = File(result.file.absolutePath)
                    val intent = Intent(this@CaptureMoment, ShareMoment::class.java)
                    intent.putExtra(SHARE_MOMENT_FILE_PATH, videoFile.absolutePath)
                    intent.putExtra(SHARE_MOMENT_FILE_TYPE, 2)
                    startActivity(intent)
                } catch (e: FileNotFoundException) {
                    TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                } catch (e: IOException) {
                    TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }
            override fun onOrientationChanged(orientation: Int) {}
            override fun onVideoRecordingStart() {}
            override fun onVideoRecordingEnd() {}
        })
    }

    private fun initializeCameraViewKit() {
        camera_view.visibility = View.GONE
        camera_view_kit.visibility = View.VISIBLE
        cameraKitView = findViewById<CameraKitView>(R.id.camera_view_kit) as CameraKitView
        cameraKitView.imageMegaPixels = 40.0f
        cameraKitView.cameraListener = object : CameraKitView.CameraListener {
            override fun onOpened() {}
            override fun onClosed() {}
        }
        cameraKitView.gestureListener = object : CameraKitView.GestureListener {
            override fun onTap(cameraKitView: CameraKitView?, x: Float, y: Float) {}
            override fun onDoubleTap(cameraKitView: CameraKitView?, x: Float, y: Float) {}
            override fun onPinch(cameraKitView: CameraKitView?, z: Float, x: Float, y: Float) {}
            override fun onLongTap(cameraKitView: CameraKitView?, x: Float, y: Float) {}
        }
        cameraKitView.errorListener = CameraKitView.ErrorListener { _, error ->
            TingToast(this@CaptureMoment, error.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
        }
    }

    private fun saveCapturedPicture(data: ByteArray) {
        val filename = "moment_${System.currentTimeMillis()}.jpeg"
        val path = File(Environment.getExternalStorageDirectory(), "Ting.com" + File.separator + "Moments" + File.separator + "Images")
        if (!path.exists()) { path.mkdirs() }
        val picture = File(path, filename)
        try {
            val outputStream = FileOutputStream(picture)
            outputStream.write(data)
            outputStream.close()

            if(isUsingCameraKit) { cameraKitView.onPause() }

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
            if(isUsingCameraKit) { cameraKitView.onResume() }
            TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
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
        if(isUsingCameraKit) { cameraKitView.onStart() }
        else { cameraView.open() }
    }

    override fun onResume() {
        super.onResume()
        if(isUsingCameraKit) { cameraKitView.onResume() }
        else { cameraView.open() }
    }

    override fun onPause() {
        if(isUsingCameraKit) { cameraKitView.onPause() }
        else { cameraView.close() }
        super.onPause()
    }

    override fun onStop() {
        if(isUsingCameraKit) { cameraKitView.onStop() }
        else { cameraView.close() }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isUsingCameraKit) { cameraKitView.onStop() }
        else { cameraView.destroy() }
        Bridge.clear(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(isUsingCameraKit) { cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults) }
        when(requestCode) {
            REQUEST_CODE_IMAGE_GALLERY -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(this, Gallery::class.java)
                    intent.putExtra("title", resources.getString(R.string.edit_user_profile_select_image))
                    intent.putExtra("mode", 2)
                    intent.putExtra("maxSelection", 1)
                    startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
                }
            }

            REQUEST_CODE_RECORD_AUDIO -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(!isUsingCameraKit) { cameraView.open() }
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_IMAGE_PICKER -> {
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
            }

            PHOTO_EDITOR_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val newFilePath = data.getStringExtra(ImageEditorIntentBuilder.OUTPUT_PATH)
                    val isImageEdit = data.getBooleanExtra(EditImageActivity.IS_IMAGE_EDITED, false)

                    try {
                        val imageFile = File(newFilePath)
                        val intent = Intent(this@CaptureMoment, ShareMoment::class.java)
                        intent.putExtra(SHARE_MOMENT_FILE_PATH, imageFile.absolutePath)
                        intent.putExtra(SHARE_MOMENT_FILE_TYPE, 1)
                        startActivity(intent)
                    } catch (e: FileNotFoundException) {
                        TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    } catch (e: IOException) {
                        TingToast(this@CaptureMoment, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_PICKER: Int = 3
        private const val PHOTO_EDITOR_REQUEST_CODE: Int = 5
        private const val REQUEST_CODE_IMAGE_GALLERY: Int = 6
        private const val REQUEST_CODE_RECORD_AUDIO: Int = 10

        public const val SHARE_MOMENT_FILE_PATH: String = "moment_file_path"
        public const val SHARE_MOMENT_FILE_TYPE: String = "moment_file_type"
    }
}
