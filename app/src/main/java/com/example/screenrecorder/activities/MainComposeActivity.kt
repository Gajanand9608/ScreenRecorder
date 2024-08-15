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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.screenrecorder.MyMediaProjectionService
import com.example.screenrecorder.R
import com.example.screenrecorder.activities.ui.theme.ScreenRecorderTheme

class MainComposeActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager : MediaProjectionManager
    private var TAG = "MainActivity"

    companion object {
        val SCREEN_CAPTURE_REQUEST_CODE = 100
    }

    private val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
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
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionResultLauncher.launch(permissions.toTypedArray())
    }


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        setContent {
            ScreenRecorderTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text("Screen Recorder")
                        }
                    )
                }, floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            requestMediaProjection()
                        },
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_play_circle_filled_24), "Floating action button.")
                    }
                }) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {

                    }
                }
            }
        }
    }

    private fun requestMediaProjection() {
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}
