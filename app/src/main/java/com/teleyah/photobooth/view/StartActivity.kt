package com.teleyah.photobooth.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.teleyah.photobooth.databinding.ActivityStartBinding
import com.teleyah.photobooth.service.GooglePhotosService
import com.teleyah.photobooth.service.SettingsService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class StartActivity : AppCompatActivity() {

    @Inject
    lateinit var googlePhotosService: GooglePhotosService

    @Inject
    lateinit var settingsService: SettingsService

    private lateinit var binding: ActivityStartBinding

    companion object {
        private const val RC_SIGN_IN = 11
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.GET_ACCOUNTS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // TODO: additional prompt if permissions refused
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
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

    private lateinit var mGoogleSignInClient : GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // TODO: move signin to settings page
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(GooglePhotosService.scopes.first(), *GooglePhotosService.scopes)
            .requestIdToken(GooglePhotosService.clientId)
            .requestServerAuthCode(GooglePhotosService.clientId,false)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions)

        binding.buttonStart.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        googleSignIn()
    }

    private fun googleSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account?.serverAuthCode == null) {
            startActivityForResult(mGoogleSignInClient.signInIntent, RC_SIGN_IN)
        } else {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    googlePhotosService.initialize(account, settingsService.albumName)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to initialize Google Photos")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            GlobalScope.launch(Dispatchers.IO) {
                googlePhotosService.initialize(account, settingsService.albumName)
            }
            Timber.i("Google sign success ${account.email}")
        } catch (e: ApiException) {
            Timber.e(e, "sign in failed ${e.statusCode}")
        }
    }
}