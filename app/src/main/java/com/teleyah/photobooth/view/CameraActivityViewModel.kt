package com.teleyah.photobooth.view

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleyah.photobooth.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CameraActivityState {
    object Preview: CameraActivityState()
    data class Countdown(val count: Int): CameraActivityState()
    object TakePicture: CameraActivityState()
    object Completed : CameraActivityState()
}

@HiltViewModel
class CameraActivityViewModel @Inject constructor(): ViewModel() {

    @Inject
    lateinit var settings: SettingsService

    private val _stateFlow = MutableStateFlow<CameraActivityState>(CameraActivityState.Preview)
    val stateFlow: StateFlow<CameraActivityState> = _stateFlow.asStateFlow()

    fun start() {
       _stateFlow.value = CameraActivityState.Preview
       startCountdown()
    }

    fun photoTaken(photo: Bitmap) {
        photos.add(photo)

        if (photos.size == settings.numberOfPhotos)
            _stateFlow.value = CameraActivityState.Completed
        else
            startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            createCountdownFlow(settings.countdownSeconds + 1)
                .conflate()
                .onEach { _stateFlow.value = CameraActivityState.Countdown(it) }
                .onCompletion { _stateFlow.value = CameraActivityState.TakePicture }
                .collect()
        }
    }

    private fun createCountdownFlow(seconds: Int): Flow<Int> =
        flow {
                (seconds downTo 1).forEach { count ->
                    emit(count)
                    delay(1000)
                }
            }

    fun reset() {
        photos.clear()
        _stateFlow.value = CameraActivityState.Preview
    }

    val photos: MutableList<Bitmap> = mutableListOf()
}
