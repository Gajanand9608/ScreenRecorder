package com.example.screenrecorder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class CustomRecorder(
    private val context : Context,
    private val mediaProjection: MediaProjection,
    private val windowManager: WindowManager
) {

    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader

    @RequiresApi(Build.VERSION_CODES.R)
    fun startRecording() {
        val windowMetrics = windowManager.currentWindowMetrics
        val width = windowMetrics.bounds.width()
        val height = windowMetrics.bounds.height()
        val metrics =  context.resources.displayMetrics
        val screenDensity = metrics.densityDpi


        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val outputFile = File(outputDir, "screen_recording.mp4")

        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile.path)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(10000000)
                setVideoFrameRate(60)
                setVideoSize(width, height)
                prepare()
            }

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
                mediaRecorder.surface, object : VirtualDisplay.Callback() {
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


            mediaRecorder.start()
            Log.d("CustomRecorder", "Recording started.")
        } catch (e: Exception) {
            Log.d("CustomRecorder", "Error starting recording: ${e.message}", e)
        }
    }

    fun stopRecording() {
        try {
            if (::mediaRecorder.isInitialized) {
                mediaRecorder.stop()
                mediaProjection.stop()
                mediaRecorder.reset()
                mediaRecorder.release()
                Log.d("CustomRecorder", "Recording stopped.")
            }
            if (::virtualDisplay.isInitialized) {
                virtualDisplay.release()
            }
        } catch (e: Exception) {
            Log.d("CustomRecorder", "Error stopping recording: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setupImageReader() {
        val windowMetrics = windowManager.currentWindowMetrics
        val width = windowMetrics.bounds.width()
        val height = windowMetrics.bounds.height()

        imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2 // Buffer count
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun captureScreenshot() {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val outputFile = File(outputDir, "screenshot.png")

        val handlerThread = HandlerThread("ScreenshotThread").apply { start() }
        val handler = Handler(handlerThread.looper)

        val surface = imageReader.surface

        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("CustomRecorder", "onStop: mediaProjection screenshot stopped")
            }
        }, null)

        val callBack = object : VirtualDisplay.Callback() {
            override fun onPaused() {
                super.onPaused()
                Log.d("CustomRecorder", "onPaused: screenshot ")
            }

            override fun onResumed() {
                super.onResumed()
                Log.d("CustomRecorder", "onResumed: screenshot ")
            }

            override fun onStopped() {
                super.onStopped()
                Log.d("CustomRecorder", "onStopped: screenshot ")
            }
        }

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenshotCapture",
            imageReader.width,
            imageReader.height,
            context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            callBack,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                val planes = image.planes
                val buffer: ByteBuffer = planes[0].buffer
                val pixelStride: Int = planes[0].pixelStride
                val rowStride: Int = planes[0].rowStride
                val rowPadding: Int = rowStride - pixelStride * image.width

                val bitmap = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                saveBitmapToFile(bitmap, outputFile)
                Log.d("CustomRecorder", "Screenshot saved: ${outputFile.path}")
            }
        }, null)

    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            context.stopService(Intent(context, MyMediaProjectionService::class.java))
        } catch (e: Exception) {
            Log.e("CustomRecorder", "Error saving screenshot: ${e.message}", e)
        } finally {
            outputStream?.close()
        }
    }
}
