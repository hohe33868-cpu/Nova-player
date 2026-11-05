package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    var isFrontCamera: Boolean = false
        private set

    var isTorchOn: Boolean = false
        private set

    suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
        val existing = cameraProvider
        if (existing != null) {
            cont.resume(existing)
            return@suspendCoroutine
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            cont.resume(provider)
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        frontFacing: Boolean = false
    ) {
        val provider = getCameraProvider()
        isFrontCamera = frontFacing

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = if (frontFacing) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            // Restore torch if back camera
            if (!frontFacing && isTorchOn) {
                camera?.cameraControl?.enableTorch(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val newFacing = !isFrontCamera
        // Torch turns off when switching to front
        if (newFacing) isTorchOn = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val selector = if (newFacing) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                isFrontCamera = newFacing
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleTorch(): Boolean {
        if (isFrontCamera || camera == null) return false
        val newState = !isTorchOn
        try {
            camera?.cameraControl?.enableTorch(newState)
            isTorchOn = newState
            return isTorchOn
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun capturePhoto(): Result<Bitmap> = suspendCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            cont.resume(Result.failure(IllegalStateException("Camera capture is not initialized")))
            return@suspendCoroutine
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val buffer = imageProxy.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        val matrix = Matrix()
                        matrix.postRotate(rotationDegrees.toFloat())
                        if (isFrontCamera) {
                            matrix.postScale(-1f, 1f) // Mirror front camera preview
                        }

                        val rotated = Bitmap.createBitmap(
                            original,
                            0,
                            0,
                            original.width,
                            original.height,
                            matrix,
                            true
                        )
                        cont.resume(Result.success(rotated))
                    } catch (e: Exception) {
                        cont.resume(Result.failure(e))
                    } finally {
                        imageProxy.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resume(Result.failure(exception))
                }
            }
        )
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
    }
}
