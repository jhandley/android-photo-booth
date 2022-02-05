package com.teleyah.photobooth.view

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.teleyah.photobooth.service.GooglePhotosService
import com.teleyah.photobooth.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class SignInState {
    object InProgress : SignInState()
    data class Success(val account: GoogleSignInAccount) : SignInState()
    data class Error(val error: Exception) : SignInState()
}

@HiltViewModel
class StartActivityViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var googlePhotosService: GooglePhotosService

    @Inject
    lateinit var settingsService: SettingsService

    private val _signInState = MutableStateFlow<SignInState>(SignInState.InProgress)
    val signInState: StateFlow<SignInState> = _signInState

    fun doGoogleSignIn(
        activity: AppCompatActivity,
        signInResultLauncher: ActivityResultLauncher<Intent>
    ) {
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account?.serverAuthCode == null) {
            signInResultLauncher.launch(createGoogleSignInClient(activity).signInIntent)
        } else {
            _signInState.value = SignInState.Success(account)
        }
    }

    fun onSignInResult(result: ActivityResult) {
        val task: Task<GoogleSignInAccount> =
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModelScope.launch {
                try {
                    googlePhotosService.login(account)
                    googlePhotosService.setAlbumName(settingsService.albumName)
                    _signInState.value = SignInState.Success(account)
                } catch (e: Exception) {
                    Timber.e(e, "Google photos login failed ${e.message}")
                    _signInState.value = SignInState.Error(e)
                }
            }
        } catch (e: ApiException) {
            Timber.e(e, "sign in failed ${e.statusCode}")
            _signInState.value = SignInState.Error(e)
        }
    }

    private fun createGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(GooglePhotosService.scopes.first(), *GooglePhotosService.scopes)
            .requestIdToken(GooglePhotosService.clientId)
            .requestServerAuthCode(GooglePhotosService.clientId, false)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, signInOptions)
    }
}