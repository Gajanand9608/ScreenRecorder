package com.example.screenrecorder.activities

import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.screenrecorder.MyMediaProjectionService
import com.example.screenrecorder.activities.ui.theme.ScreenRecorderTheme

class MainComposeActivity : ComponentActivity() {

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
        enableEdgeToEdge()
        requestPermissions()
        setContent {
            ScreenRecorderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ScreenRecorderTheme {
        Greeting("Android")
    }
}