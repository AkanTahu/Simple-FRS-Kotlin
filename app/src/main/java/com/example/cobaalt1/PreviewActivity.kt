package com.example.cobaalt1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        val imagePath = intent.getStringExtra("imagePath")
        val imageView: ImageView = findViewById(R.id.imageView)

        val imgFile = File(imagePath!!)
        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)

            // Menyesuaikan ukuran gambar agar sesuai dengan dimensi ImageView (500px)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, false)
            imageView.setImageBitmap(resizedBitmap)
        }
    }
}


