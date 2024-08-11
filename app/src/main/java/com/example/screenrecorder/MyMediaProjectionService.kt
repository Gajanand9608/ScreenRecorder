package com.example.screenrecorder

import android.app.Activity.RESULT_CANCELED
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.File

class MyMediaProjectionService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var mMediaRecorder: MediaRecorder
    private lateinit var virtualDisplay: VirtualDisplay

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        myForegroundService()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startScreenRecording() {
        // Get the WindowManager
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val windowMetrics = windowManager.currentWindowMetrics
        val width = windowMetrics.bounds.width()
        val height = windowMetrics.bounds.height()

        val metrics = resources.displayMetrics
        val screenDensity = metrics.densityDpi

        mMediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        val outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val outputFile = File(outputDir, "screen_recording.mp4")

        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mMediaRecorder.setOutputFile(outputFile.path)
            Log.d("Gajanand", "Output file path: ${outputFile.path}")
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            mMediaRecorder.setVideoEncodingBitRate(10000000)
            mMediaRecorder.setVideoFrameRate(60)
            mMediaRecorder.setVideoSize(width, height)
            mMediaRecorder.prepare()

            // Register callback for MediaProjection
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d("Gajanand", "onStop: mediaProjection stopeed")
//                    stopScreenRecording()
                }
            }, null)




            // Create a VirtualDisplay
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width, height, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.surface, object : VirtualDisplay.Callback() {
                    override fun onPaused() {
                        super.onPaused()
                        Log.d("Gajanand", "onPaused: ")
                    }

                    override fun onResumed() {
                        super.onResumed()
                        Log.d("Gajanand", "onResumed: ")
                    }

                    override fun onStopped() {
                        super.onStopped()
                        Log.d("Gajanand", "onStopped: ")
                    }
                }, null
            )


            mMediaRecorder.start()

            // Schedule stop recording after 10 seconds
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                stopScreenRecording()
            }, 10000) // 10 seconds in milliseconds

        } catch (e: Exception) {
            Log.d("Gajanand", "startScreenRecording: ${e.message} ", e)
        }
    }

    private fun stopScreenRecording() {
        try {
            if (::mMediaRecorder.isInitialized) {
                mMediaRecorder.stop()
                mediaProjection.stop()
                mMediaRecorder.reset() // Reset the MediaRecorder to release resources
                mMediaRecorder.release()
                Log.d("Gajanand", "Screen recording stopped.")
            }
            if (::virtualDisplay.isInitialized) {
                virtualDisplay.release()
            }
        } catch (e: IllegalStateException) {
            Log.d("Gajanand", "stopScreenRecording: IllegalStateException - ${e.message}", e)
        } catch (e: RuntimeException) {
            Log.d("Gajanand", "stopScreenRecording: RuntimeException - ${e.message}", e)
        } catch (e: Exception) {
            Log.d("Gajanand", "stopScreenRecording: ${e.message}", e)
        } finally {
            // Clean up the MediaRecorder reference
            if (::mMediaRecorder.isInitialized) {
                mMediaRecorder.release()
            }
        }
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
        val resultCode =
            intent?.getIntExtra("resultCode", RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data")

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
        Log.d("Gajanand", "onStartCommand: resultCode $resultCode ")
        Log.d("Gajanand", "onStartCommand: data $data ")

        startScreenRecording()

        return START_STICKY
    }


}