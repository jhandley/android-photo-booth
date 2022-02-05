package com.teleyah.photobooth.view

import androidx.lifecycle.ViewModel
import com.teleyah.photobooth.model.PhotoCollage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PrintDownloadActivityViewModel @Inject constructor(): ViewModel() {

    lateinit var collage: PhotoCollage

}