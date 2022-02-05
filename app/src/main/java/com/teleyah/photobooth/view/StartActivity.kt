package com.teleyah.photobooth.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.teleyah.photobooth.databinding.ActivityStartBinding
import com.teleyah.photobooth.service.SettingsService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class StartActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsService: SettingsService

    private lateinit var binding: ActivityStartBinding

    private val viewModel: StartActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.buttonStart.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        val signInResultHandler =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                viewModel.onSignInResult(it)
            }

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                if (viewModel.signInState.value !is SignInState.Success)
                    viewModel.doGoogleSignIn(this@StartActivity, signInResultHandler)

                viewModel.signInState.collect { state ->

                    when (state) {
                        is SignInState.Success -> {
                            binding.buttonStart.visibility = View.VISIBLE
                            binding.progressBar.visibility = View.INVISIBLE
                            binding.progressText.visibility = View.INVISIBLE
                        }
                        is SignInState.Error -> {
                            binding.buttonStart.visibility = View.VISIBLE
                            binding.progressBar.visibility = View.INVISIBLE
                            binding.progressText.visibility = View.INVISIBLE
                            Snackbar.make(
                                binding.layoutStart,
                                "Failed to sign in to Google Photos.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        SignInState.InProgress -> {
                            binding.buttonStart.visibility = View.INVISIBLE
                            binding.progressBar.visibility = View.VISIBLE
                            binding.progressText.visibility = View.VISIBLE
                        }
                    }
                }

            }
        }
    }
}