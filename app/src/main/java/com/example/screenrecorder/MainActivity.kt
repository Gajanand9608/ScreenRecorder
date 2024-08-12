package com.example.screenrecorder

import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.screenrecorder.databinding.ActivityMain2Binding
import com.example.screenrecorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var mediaProjectionManager : MediaProjectionManager
    lateinit var mediaProjection : MediaProjection
    private var TAG = "MainActivity"

    companion object {
        val SCREEN_CAPTURE_REQUEST_CODE = 100
    }


    val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            setMediaProjectionPermissionGranted() // Store that permission has been granted
            val serviceIntent = Intent(this, MyMediaProjectionService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { isGranted: Boolean ->
//        if (isGranted) {
//            requestMediaProjection()
//        } else {
//            Log.d("Gajanand", "Audio permission denied.")
//            Toast.makeText(this, "Please give Audio permission ", Toast.LENGTH_SHORT).show()
//        }
//    }

    private val permissionResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
        if (permissionResult.any { !it.value }) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }else{
            requestMediaProjection()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionResultLauncher.launch(permissions.toTypedArray())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()

        binding.moveToNextScreen.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

    private fun requestMediaProjection() {
//        if (!hasMediaProjectionPermission()) {
            mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
//        } else {
//            startScreenRecordingServiceDirectly() // Directly start the service if permission was already granted
//        }
    }

    private fun startScreenRecordingServiceDirectly() {
        val serviceIntent = Intent(this, MyMediaProjectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun hasMediaProjectionPermission(): Boolean {
        val sharedPreferences = getSharedPreferences("MediaProjectionPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("hasMediaProjectionPermission", false)
    }

    private fun setMediaProjectionPermissionGranted() {
        val sharedPreferences = getSharedPreferences("MediaProjectionPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("hasMediaProjectionPermission", true)
            apply()
        }
    }
}