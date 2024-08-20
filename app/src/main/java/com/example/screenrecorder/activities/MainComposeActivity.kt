package com.example.screenrecorder.activities

import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.screenrecorder.MainViewModel
import com.example.screenrecorder.MyMediaProjectionService
import com.example.screenrecorder.R
import com.example.screenrecorder.activities.ui.theme.ScreenRecorderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainComposeActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var mediaProjectionManager : MediaProjectionManager
    private var TAG = "MainActivity"
    private var isRecording = true

    private val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val serviceIntent = Intent(this, MyMediaProjectionService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
                putExtra("isRecording", isRecording)
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
            val scope = rememberCoroutineScope()
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
                            scope.launch(Dispatchers.IO) {
                                val currentStatus = viewModel.isRecordingState.value
                                viewModel.setRecordingStatus(!currentStatus)
                                if(currentStatus){
                                    // stop recording
                                    stopService(Intent(this@MainComposeActivity, MyMediaProjectionService::class.java))
                                }else{
                                    // start recording
                                    isRecording = true
                                    requestMediaProjection()
                                }
                            }
                        },
                    ) {
                        val drawable = if(viewModel.isRecordingState.value) R.drawable.ic_pause_circle_24 else R.drawable.ic_play_circle_filled_24
                        Icon(painter = painterResource(id = drawable ), contentDescription = "")
                    }
                }) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                val currentStatus = viewModel.isTakingScreenShot.value
                                if(currentStatus == null || currentStatus == false){
                                    viewModel.setScreenShotStatus(true)
                                    isRecording = false
                                    requestMediaProjection()
                                }else{
                                    viewModel.setScreenShotStatus(false)
                                    stopService(Intent(this@MainComposeActivity, MyMediaProjectionService::class.java))
                                }
                            }
                        }) {
                            Text(text = "Take screenshot")
                        }
                        if(viewModel.isRecordingState.value){
                            Toast.makeText(this,"Recording started", Toast.LENGTH_LONG).show()
                        }else{
                            Toast.makeText(this,"Recording stopped", Toast.LENGTH_LONG).show()
                        }
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
