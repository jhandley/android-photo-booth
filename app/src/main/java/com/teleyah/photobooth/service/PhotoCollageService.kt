package com.teleyah.photobooth.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.teleyah.photobooth.model.PhotoCollage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class PhotoCollageService @Inject constructor(@ApplicationContext private val context: Context) {

    @Inject lateinit var settingsService: SettingsService

    fun createPhotoGrid(photos: List<Bitmap>): PhotoCollage {

        if (photos.size != 4)
            throw IllegalArgumentException("Must have four photos")

        val collageBitmap = settingsService.background.copy(settingsService.background.config,  true)

        val border = 5 * collageBitmap.width/100
        val columnSpacing = 5 * collageBitmap.width/100
        val rowSpacing = 5 * collageBitmap.width/100
        val topRowImageWidth = (collageBitmap.width - (2 * border + 2 * columnSpacing))/3
        val aspectRatio = with(photos[0]) { width.toDouble()/height.toDouble() }
        val topRowImageHeight = (topRowImageWidth / aspectRatio).roundToInt()

        val bottomRowImageHeight = collageBitmap.height - (2 * border + topRowImageHeight + rowSpacing)
        val bottomRowImageWidth = (bottomRowImageHeight * aspectRatio).roundToInt()

        val canvas = Canvas(collageBitmap)
        photos.take(3).forEachIndexed { index, bitmap ->
            val left = border + index * (columnSpacing + topRowImageWidth)
            val top = border
            val destination = makeRect(left, top, topRowImageWidth, topRowImageHeight)
            canvas.drawBitmapToRect(bitmap, destination)
        }

        val left = border
        val top = border + topRowImageHeight + rowSpacing
        val destination = makeRect(left, top, bottomRowImageWidth, bottomRowImageHeight)
        canvas.drawBitmapToRect(photos[3], destination)

        val albumDir = File(context.filesDir, settingsService.albumName)
        if (!albumDir.exists())
            albumDir.mkdir()
        val name = "PhotoBooth_${System.currentTimeMillis()}"
        val imageFile = File(albumDir, "$name.jpg")
        collageBitmap.writeToFile(imageFile)
        return PhotoCollage(name, imageFile)
    }


    private fun Bitmap.writeToFile(file: File) {
        FileOutputStream(file).use {
            compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    }

    private fun Canvas.drawBitmapToRect(bitmap: Bitmap, destination: Rect) =
        drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), destination, null)

    private fun makeRect(left: Int, top: Int, width: Int, height: Int) =
        Rect(left, top, left + width, top + height)
}