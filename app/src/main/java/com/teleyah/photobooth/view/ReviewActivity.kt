package com.teleyah.photobooth.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.teleyah.photobooth.databinding.ActivityReviewBinding
import com.teleyah.photobooth.model.PhotoCollage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding

    private val viewModel: ReviewActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.collage = intent.getSerializableExtra("photo")!! as PhotoCollage
        Glide.with(this).load(viewModel.collage.path).into(binding.imagePreview)

        binding.buttonRetake.setOnClickListener {
            // Go back to previous activity to take photos again
            finish()
        }

        binding.buttonDone.setOnClickListener {

            viewModel.save()

            startActivity(
                Intent(this,PrintDownloadActivity::class.java)
                    .apply { putExtra("photo", viewModel.collage) }
            )
        }

    }
}