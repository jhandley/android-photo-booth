package com.teleyah.photobooth.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsService  @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val DEFAULT_NUMBER_OF_PHOTOS_TO_TAKE = 4
        private const val DEFAULT_COUNTDOWN_SECONDS_BETWEEN_PHOTOS = 3
    }

    val numberOfPhotos = DEFAULT_NUMBER_OF_PHOTOS_TO_TAKE

    val countdownSeconds = DEFAULT_COUNTDOWN_SECONDS_BETWEEN_PHOTOS

    val background : Bitmap by lazy {
        context.assets.open("backgrounds/80sBackground.png").use {
            BitmapFactory.decodeStream(it)
        }
    }

    val albumName = "Josh's 50th Birthday"
}