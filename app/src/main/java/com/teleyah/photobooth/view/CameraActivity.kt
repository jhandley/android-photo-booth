package com.teleyah.photobooth.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaActionSound
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.teleyah.photobooth.R
import com.teleyah.photobooth.databinding.ActivityCameraBinding
import com.teleyah.photobooth.service.PhotoCollageService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    @Inject
    lateinit var photoCollageService: PhotoCollageService

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: CameraActivityViewModel by viewModels()
    private val mediaActionSound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()

            binding.buttonStart.setOnClickListener {
                if (viewModel.stateFlow.value is CameraActivityState.Preview)
                    viewModel.start()
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
            // TODO - start camera & add listener after perms granted
        }

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reset()
                viewModel.stateFlow.collect { state ->

                    binding.buttonStart.visibility =
                        if (state is CameraActivityState.Preview) View.VISIBLE else View.GONE

                    when (state) {
                        is CameraActivityState.Preview -> {
                            binding.cameraPreview.visibility = View.VISIBLE
                            binding.imageCountdown.setImage(R.drawable.ic_ready)
                            binding.imageCountdown.visibility = View.VISIBLE
                        }
                        is CameraActivityState.Countdown -> {
                            val countdownImage = when (state.count) {
                                1 -> R.drawable.ic_1
                                2 -> R.drawable.ic_2
                                3 -> R.drawable.ic_3
                                else -> null
                            }
                            if (countdownImage == null) {
                                Timber.i("Hide image ${state.count}")
                                binding.imageCountdown.visibility = View.INVISIBLE
                            } else {
                                Timber.i("Show image ${state.count}")
                                binding.imageCountdown.setImage(countdownImage)
                                binding.imageCountdown.visibility = View.VISIBLE
                            }
                        }
                        is CameraActivityState.TakePicture -> {
                            binding.imageCountdown.visibility = View.INVISIBLE
                            Timber.i("Take picture")
                            takePhoto()
                            Timber.i("Done take picture")
                        }
                        is CameraActivityState.Completed -> {
                            binding.cameraPreview.visibility = View.INVISIBLE
                            val collage = photoCollageService.createPhotoGrid(viewModel.photos)
                            startActivity(
                                Intent(
                                    this@CameraActivity,
                                    ReviewActivity::class.java
                                ).apply { putExtra("photo", collage) })
                        }
                    }
                }
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun ImageView.setImage(resourceId: Int) =
        Glide.with(this@CameraActivity)
            .load(resourceId)
            .into(this)

    private fun takePhoto() {

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                image.use {
                    viewModel.photoTaken(it.toBitmap())
                }
            }
        }
        )
    }

    private fun startCamera() {
        binding.cameraPreview.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}