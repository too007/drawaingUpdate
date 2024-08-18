package com.example.drawaingupdate

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CropActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crop)
//        val imageView = findViewById<ImageView>(R.id.image_view)
//        val imageUriString = intent.getStringExtra("imageUri")
//        val imageUri: Uri? = imageUriString?.let { Uri.parse(it) }
    }
}

