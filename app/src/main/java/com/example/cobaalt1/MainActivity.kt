package com.example.cobaalt1

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Size
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.ImageCapture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var instructionText: TextView
    private lateinit var faceDetectionButton: Button
    private lateinit var faceRecognitionButton: Button
    private lateinit var generateDatasetButton: Button

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 10
    }

    private var captureCount = 0
    private val instructions = listOf(
        "Tolong menghadap ke tengah",
        "Tolong wajah menghadap kanan sedikit",
        "Tolong wajah menghadap kiri sedikit"
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        instructionText = findViewById(R.id.instructionText)
        faceDetectionButton = findViewById(R.id.faceDetectionButton)
        faceRecognitionButton = findViewById(R.id.faceRecognitionButton)
        generateDatasetButton = findViewById(R.id.generateDatasetButton)

        // Minta izin kamera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE)
        }

        faceDetectionButton.setOnClickListener {
            // Mulai countdown dan face detection
            startCountdown()
        }
        faceRecognitionButton.setOnClickListener {
            startCountdownRecog()
        }

        generateDatasetButton.setOnClickListener {
            startCountdownGenerate()
        }

    }

    private fun startCountdown() {
        val countdownTimer = object : CountDownTimer(7000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update text untuk countdown
                instructionText.text = "Countdown: ${millisUntilFinished / 1000} detik lagi"
            }

            override fun onFinish() {
                // Gambar otomatis setelah countdown selesai
                instructionText.text = "Mempersiapkan..."
                captureImage()
            }
        }
        countdownTimer.start()
    }

    private fun startCountdownRecog() {
        val countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update text untuk countdown
                instructionText.text = "Countdown: ${millisUntilFinished / 1000} detik lagi"
            }

            override fun onFinish() {
                // Gambar otomatis setelah countdown selesai
                instructionText.text = "Mempersiapkan..."
                captureImageRecog()
            }
        }
        countdownTimer.start()
    }

    private fun startCountdownGenerate() {
        val countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update text untuk countdown
                instructionText.text = "Countdown: ${millisUntilFinished / 1000} detik lagi"
            }

            override fun onFinish() {
                // Gambar otomatis setelah countdown selesai
                instructionText.text = "Mempersiapkan..."
                captureImageGenerate()
            }
        }
        countdownTimer.start()
    }

    private fun captureImage() {
        // Ambil gambar menggunakan ImageCapture
        val photoFile = File(externalMediaDirs.first(), "${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val msg = "Foto berhasil diambil: ${photoFile.absolutePath}"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                // Pindah ke activity berikutnya
                processFaceDetection(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(applicationContext, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun captureImageRecog() {
        val photoFile = File(externalMediaDirs.first(), "${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                val msg = "Foto berhasil diambil: ${photoFile.absolutePath}"
//                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()

                // Kirim gambar ke server Flask setelah diambil
                processFaceDetectionForRecog(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(applicationContext, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startDatasetCapture() {
        captureCount = 0
        captureNextImage()
    }

    private fun captureNextImage() {
        if (captureCount < 3) {
            // Update instruksi
            instructionText.text = instructions[captureCount]

            // Mulai countdown 7 detik
            val countdownTimer = object : CountDownTimer(7000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    instructionText.text = "${instructions[captureCount]} - Countdown: ${millisUntilFinished / 1000} detik lagi"
                }

                override fun onFinish() {
                    // Ambil gambar setelah countdown selesai
                    instructionText.text = "Mengambil gambar..."
                    captureImageGenerate()
                }
            }
            countdownTimer.start()
        } else {
            instructionText.text = "Dataset telah berhasil diambil!"
        }
    }


    private fun captureImageGenerate() {
        val photoFile = File(externalMediaDirs.first(), "${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                // Kirim gambar ke server Flask setelah diambil
                processFaceDetectionForGenerate(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(applicationContext, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun sendImageToFlask(photoFile: File) {
        // Convert file ke MultipartBody.Part
        val requestFile = RequestBody.create("image/jpg".toMediaTypeOrNull(), photoFile)
        val body = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)

        // Retrofit instance dan API request
        val apiService = RetrofitClient.retrofitInstance.create(ApiService::class.java)
        val call = apiService.uploadImage(body)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Toast.makeText(applicationContext, "AWOK1", Toast.LENGTH_SHORT).show()
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "AWOK2", Toast.LENGTH_SHORT).show()
                    val responseBody = response.body()?.string()
                    Toast.makeText(applicationContext, "Hasil: $responseBody", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "Gagal mengirim gambar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(applicationContext, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerFace(photoFile: File, userName: String) {
        // Convert file ke MultipartBody.Part
        val requestFile = RequestBody.create("image/jpg".toMediaTypeOrNull(), photoFile)
        val body = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)

        // Request body untuk nama pengguna
        val name = RequestBody.create("text/plain".toMediaTypeOrNull(), userName)

        // Retrofit instance dan API request
        val apiService = RetrofitClient.retrofitInstance.create(ApiService::class.java)
        val call = apiService.registerFace(body, name)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Toast.makeText(applicationContext, "Response: $responseBody", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "Gagal registrasi wajah", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(applicationContext, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun processFaceDetection(photoFile: File) {
        // Membaca gambar untuk Face Detection
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        // Inisialisasi Face Detector
        val faceDetector: FaceDetector = FaceDetection.getClient()

        // Melakukan deteksi wajah
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    // Ambil wajah pertama yang terdeteksi (bisa disesuaikan untuk deteksi beberapa wajah)
                    val face = faces[0]
                    val bounds: Rect = face.boundingBox
                    val croppedBitmap = cropBitmap(bitmap, bounds)
                    val croppedFile = File(externalMediaDirs.first(), "cropped_${photoFile.name}")

                    // Simpan gambar cropped (dengan wajah saja)
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, croppedFile.outputStream())

                    // Kirim gambar cropped ke PreviewActivity
                    showImagePreview(croppedFile)
                } else {
                    Toast.makeText(applicationContext, "Tidak ada wajah yang terdeteksi", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Deteksi wajah gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processFaceDetectionForRecog(photoFile: File) {
        // Membaca gambar untuk Face Detection
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        // Inisialisasi Face Detector
        val faceDetector: FaceDetector = FaceDetection.getClient()

        // Melakukan deteksi wajah
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    // Ambil wajah pertama yang terdeteksi (bisa disesuaikan untuk deteksi beberapa wajah)
                    val face = faces[0]
                    val bounds: Rect = face.boundingBox
                    val croppedBitmap = cropBitmap(bitmap, bounds)
                    val croppedFile = File(externalMediaDirs.first(), "cropped_${photoFile.name}")

                    // Simpan gambar cropped (dengan wajah saja)
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, croppedFile.outputStream())

                    // Kirim gambar cropped ke PreviewActivity
                    sendImageToFlask(croppedFile)
                } else {
                    Toast.makeText(applicationContext, "Tidak ada wajah yang terdeteksi", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Deteksi wajah gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processFaceDetectionForGenerate(photoFile: File) {
        // Membaca gambar untuk Face Detection
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        // Inisialisasi Face Detector
        val faceDetector: FaceDetector = FaceDetection.getClient()

        // Melakukan deteksi wajah
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    // Ambil wajah pertama yang terdeteksi (bisa disesuaikan untuk deteksi beberapa wajah)
                    val face = faces[0]
                    val bounds: Rect = face.boundingBox
                    val croppedBitmap = cropBitmap(bitmap, bounds)
                    val croppedFile = File(externalMediaDirs.first(), "cropped_${photoFile.name}")

                    // Simpan gambar cropped (dengan wajah saja)
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, croppedFile.outputStream())

                    // Kirim gambar cropped ke PreviewActivity
                    registerFace(croppedFile, "isal")
                    captureCount++
                    captureNextImage()
                } else {
                    Toast.makeText(applicationContext, "Tidak ada wajah yang terdeteksi", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Deteksi wajah gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cropBitmap(bitmap: Bitmap, bounds: Rect): Bitmap {
        // Potong gambar sesuai dengan area wajah
        return Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
    }

    private fun showImagePreview(photoFile: File) {
        // Pindah ke Activity berikutnya dan tampilkan gambar
        val intent = Intent(this, PreviewActivity::class.java).apply {
            putExtra("imagePath", photoFile.absolutePath)
        }
        startActivity(intent)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Pilih kamera depan
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

            // Buat pratinjau
            preview = Preview.Builder()
                .setTargetResolution(Size(500, 500)) // Atur resolusi pratinjau
                .build()

            // ImageCapture untuk mengambil gambar
            imageCapture = ImageCapture.Builder()
                .setTargetRotation(windowManager.defaultDisplay.rotation)
                .build()

            // Ganti dengan setSurfaceProvider dari PreviewView
            preview.setSurfaceProvider(previewView.surfaceProvider)

            try {
                // Bind kamera ke lifecycle
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                // Tangani error
            }
        }, ContextCompat.getMainExecutor(this))
    }
}



