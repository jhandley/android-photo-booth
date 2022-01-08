package com.teleyah.photobooth.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleyah.photobooth.model.PhotoCollage
import com.teleyah.photobooth.service.MediaStoreService
import com.teleyah.photobooth.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrintDownloadActivityViewModel @Inject constructor(): ViewModel() {

    lateinit var collage: PhotoCollage

}