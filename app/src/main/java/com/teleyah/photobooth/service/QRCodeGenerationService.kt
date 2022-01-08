package com.teleyah.photobooth.service

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeGenerationService @Inject constructor() {

    private val encoder = BarcodeEncoder()

    fun generateQrCode(content: String, width: Int, height: Int): Bitmap =
        encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, width, height)
}