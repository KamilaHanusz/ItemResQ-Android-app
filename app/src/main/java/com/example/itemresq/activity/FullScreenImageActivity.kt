package com.example.itemresq.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.itemresq.databinding.ActivityFullScreenImageBinding
import com.squareup.picasso.Picasso

class FullScreenImageActivity : AppCompatActivity() {

    private lateinit var binding : ActivityFullScreenImageBinding
    private lateinit var imageUrls: List<String>
    private var currentImageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageUrls = intent.getStringArrayListExtra("IMAGE_URLS") ?: listOf()
        currentImageIndex = intent.getIntExtra("CURRENT_IMAGE_INDEX", 0)
        if (imageUrls.isNotEmpty()) {
            loadImage()
        }

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.previousPicBtn.setOnClickListener {
            navigateToPreviousImage()
        }

        binding.nextPicBtn.setOnClickListener {
            navigateToNextImage()
        }

        updateArrowVisibility()
    }

    private fun navigateToPreviousImage() {
        if (currentImageIndex > 0) {
            currentImageIndex--
            loadImage()
        }
        updateArrowVisibility()
    }

    private fun navigateToNextImage() {
        if (currentImageIndex < imageUrls.size - 1) {
            currentImageIndex++
            loadImage()
        }
        updateArrowVisibility()
    }

    private fun loadImage() {
        Picasso.get().load(imageUrls[currentImageIndex]).into(binding.image)
    }

    private fun updateArrowVisibility() {
        if (currentImageIndex == 0) {
            binding.previousPicBtn.visibility = View.GONE
        } else {
            binding.previousPicBtn.visibility = View.VISIBLE
        }

        if (currentImageIndex == imageUrls.size - 1) {
            binding.nextPicBtn.visibility = View.GONE
        } else {
            binding.nextPicBtn.visibility = View.VISIBLE
        }
    }
}
