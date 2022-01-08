package com.teleyah.photobooth.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleyah.photobooth.model.PhotoCollage
import com.teleyah.photobooth.service.GooglePhotosService
import com.teleyah.photobooth.service.MediaStoreService
import com.teleyah.photobooth.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class ReviewActivityViewModel @Inject constructor(): ViewModel() {

    @Inject
    lateinit var settingsService: SettingsService

    @Inject
    lateinit var mediaStoreService: MediaStoreService

    @Inject
    lateinit var googlePhotosService: GooglePhotosService

    lateinit var collage: PhotoCollage

    fun save() {
        val path = collage.path
        val name = collage.name
        viewModelScope.launch {

            launch {
                try {
                    mediaStoreService.insertImage(path, name, settingsService.albumName)
                } catch (e: Exception) {
                    Timber.e(e, "Error copying file $path to media store")
                }
            }
            try {
                googlePhotosService.upload(path)
            } catch (e: Exception) {
                Timber.e(e, "Error uploading file $path to Google Photos")
            }
        }
    }
}