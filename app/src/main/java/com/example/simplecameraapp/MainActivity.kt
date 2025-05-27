//package com.example.simplecameraapp
//
//import android.Manifest
//import android.app.Activity
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.os.Bundle
//import android.provider.MediaStore
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.Toast
//// Import AppCompatActivity
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//
//// Import for Activity Result APIs
//import androidx.activity.result.contract.ActivityResultContracts
//
//class MainActivity : AppCompatActivity() {
//    private lateinit var imageView: ImageView
//    private lateinit var takePictureButton: Button
//
//    // Modern Activity Result APIs
//    private val cameraLauncher = registerForActivityResult(
//        // Added import for ActivityResultContracts.StartActivityForResult
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            // Get the captured image bitmap from the result data
//            val photo = result.data?.extras?.get("data") as? Bitmap
//            photo?.let {
//                // Set the captured image to the ImageView
//                imageView.setImageBitmap(it)
//            } ?: run {
//                // Show a toast message if image capture failed
//                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            // Optional: Handle cases where the result code is not OK (e.g., user cancelled)
//            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private val permissionLauncher = registerForActivityResult(
//        // Corrected: Added import for ActivityResultContracts.RequestPermission
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (isGranted) {
//            // If permission is granted, open the camera
//            openCamera()
//        } else {
//            // If permission is denied, show a toast message
//            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // Set the layout for the activity
//        setContentView(R.layout.activity_main)
//
//        // Initialize ImageView and Button
//        imageView = findViewById(R.id.image_view)
//        takePictureButton = findViewById(R.id.btn_take_picture)
//
//        // Set click listener for the button
//        takePictureButton.setOnClickListener {
//            // Check camera permission and open camera when the button is clicked
//            checkCameraPermissionAndOpenCamera()
//        }
//    }
//
//    private fun checkCameraPermissionAndOpenCamera() {
//        when {
//            // Check if camera permission is already granted
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.CAMERA
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                // If granted, open the camera
//                openCamera()
//            }
//            else -> {
//                // If not granted, request the camera permission
//                permissionLauncher.launch(Manifest.permission.CAMERA)
//            }
//        }
//    }
//
//    private fun openCamera() {
//        // Create an intent to capture an image
//        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        // Check if there is a camera app available to handle the intent
//        if (cameraIntent.resolveActivity(packageManager) != null) {
//            // Launch the camera intent using the Activity Result API
//            cameraLauncher.launch(cameraIntent)
//        } else {
//            // Show a toast message if no camera app is found
//            Toast.makeText(this, "No camera app found on this device", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//}

package com.example.simplecameraapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var cameraPreview: PreviewView
    private lateinit var saveButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var takePictureButton: com.google.android.material.floatingactionbutton.FloatingActionButton

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted){
            startCamera()
        } else{
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        takePictureButton = findViewById(R.id.fab_take_picture)
        cameraPreview = findViewById(R.id.camera_preview)
        saveButton = findViewById(R.id.btn_save)
        imagePreview = findViewById(R.id.image_view)

        cameraExecutor = Executors.newSingleThreadExecutor()

        saveButton.visibility = Button.GONE
        imagePreview.visibility = ImageView.GONE

        takePictureButton.setOnClickListener { takePhoto() }
        saveButton.setOnClickListener { saveToGallery() }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(cameraPreview.surfaceProvider)

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val fileName =
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    savedUri?.let {
                        imagePreview.visibility = ImageView.VISIBLE
                        saveButton.visibility = Button.VISIBLE
                        imagePreview.setImageURI(it)
                        Toast.makeText(this@MainActivity, "Photo captured", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun saveToGallery() {
        // Image is already saved to gallery via MediaStore in takePhoto()
        Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
        resetUI()
    }

    private fun resetUI() {
        imagePreview.visibility = ImageView.GONE
        saveButton.visibility = Button.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

// complete