package com.teleyah.photobooth.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.print.PrintHelper
import com.bumptech.glide.Glide
import com.teleyah.photobooth.databinding.ActivityPrintDownloadBinding
import com.teleyah.photobooth.model.PhotoCollage
import com.teleyah.photobooth.service.GooglePhotosService
import com.teleyah.photobooth.service.QRCodeGenerationService
import com.teleyah.photobooth.service.SettingsService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrintDownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrintDownloadBinding

    private val viewModelDownload: PrintDownloadActivityViewModel by viewModels()

    @Inject
    lateinit var settingsService: SettingsService

    @Inject
    lateinit var qrCodeService: QRCodeGenerationService

    @Inject
    lateinit var googlePhotosService: GooglePhotosService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrintDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModelDownload.collage = intent.getSerializableExtra("photo")!! as PhotoCollage
        Glide.with(this).load(viewModelDownload.collage.path).into(binding.imagePreview)

        val qrCode = qrCodeService.generateQrCode(googlePhotosService.sharedAlbumUrl, 100, 100)
        Glide.with(this).load(qrCode).into(binding.imageQrCode)

        binding.buttonDone.setOnClickListener {
            startActivity(
                Intent(
                    this@PrintDownloadActivity,
                    StartActivity::class.java
                ).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
        }

        binding.buttonPrint.setOnClickListener {
            PrintHelper(this)
                .apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
                .printBitmap(settingsService.albumName, viewModelDownload.collage.path.toUri())
        }
     }
}