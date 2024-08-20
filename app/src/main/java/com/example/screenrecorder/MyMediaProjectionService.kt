package com.example.screenrecorder

import android.app.Activity.RESULT_CANCELED
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class MyMediaProjectionService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var customRecorder: CustomRecorder

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        myForegroundService()
    }

    private fun myForegroundService() {
        val notificationChannelId = "MEDIA_PROJECTION_SERVICE_CHANNEL"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Screen Recording",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Screen Recording")
            .setContentText("Recording screen...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", RESULT_CANCELED) ?: return START_NOT_STICKY
        val isRecording = intent.getBooleanExtra("isRecording", true)
        val data = intent.getParcelableExtra<Intent>("data")


        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)

        customRecorder = CustomRecorder(this,mediaProjection, getSystemService(WINDOW_SERVICE) as WindowManager)
        if(isRecording){
            Log.d("Gajanand", "onStartCommand: isRecording $isRecording ")
            customRecorder.startRecording()
        }else{
            Log.d("Gajanand", "onStartCommand: isRecording $isRecording ")
            customRecorder.setupImageReader()
            Handler().postDelayed({
                customRecorder.captureScreenshot()
            }, 4000)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        customRecorder.stopRecording()
    }

}