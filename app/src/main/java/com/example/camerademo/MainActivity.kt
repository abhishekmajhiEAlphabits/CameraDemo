package com.example.camerademo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.TextureView
import android.widget.Toast
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var processCameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider

    //    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            init()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }


        // Set up the listeners for take photo and video capture buttons
//        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        //viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }


    }

    @SuppressLint("RestrictedApi")
    private fun init() {
        CameraX.initialize(this, Camera2Config.defaultConfig())
        processCameraProviderFuture = ProcessCameraProvider.getInstance(this)
        processCameraProviderFuture.addListener(Runnable {
            processCameraProvider = processCameraProviderFuture.get()
            viewFinder.post { setupCamera() }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {}

    private fun captureVideo() {}

//    private fun startCamera() {
//        Log.d(TAG, "camera workingss..")
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener(Runnable {
//
//                // Used to bind the lifecycle of cameras to the lifecycle owner
//                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//                // Preview
//                val preview = Preview.Builder()
//                    .build()
//                    .also {
//                        it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
//                    }
//
//                imageCapture = ImageCapture.Builder().build()
//
//                // Select back camera as a default
//                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
////            val cameraSelector = CameraSelector.Builder()
////                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
////                .build()
//
//                try {
//                    // Unbind use cases before rebinding
//                    cameraProvider.unbindAll()
//
//                    // Bind use cases to camera
//                    cameraProvider.bindToLifecycle(
//                        this, cameraSelector, preview
//
//                    )
//                    Log.d(TAG, "camera working..")
//
//                } catch (exc: Exception) {
//                    Log.e(TAG, "Use case binding failed", exc)
//                }
//
//
//
//        }, ContextCompat.getMainExecutor(this))
//    }

//    private fun startCamera() {
//        val metrics = DisplayMetrics().also { viewBinding.texture.display.getRealMetrics(it) }
//        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
//        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
//
//        val previewConfig = PreviewConfig.Builder().apply {
//            setLensFacing(lensFacing)
//            setTargetResolution(screenSize)
//            setTargetAspectRatio(screenAspectRatio)
//            setTargetRotation(windowManager.defaultDisplay.rotation)
//            setTargetRotation(texture.display.rotation)
//        }.build()
//
//        val preview = Preview.Builder()
//                    .build()
//
//        val preview = Preview(previewConfig)
//        preview.setOnPreviewOutputUpdateListener {
//            texture.surfaceTexture = it.surfaceTexture
//            updateTransform()
//        }
//    }


    private fun setupCamera() {
        processCameraProvider.unbindAll()
        val camera = processCameraProvider.bindToLifecycle(
            this,
            CameraSelector.DEFAULT_BACK_CAMERA,
            buildPreviewUseCase(),
//            buildImageCaptureUseCase()
            buildImageAnalysisUseCase()
        )
        setupTapForFocus(camera.cameraControl)
    }


    private fun buildPreviewUseCase(): Preview {
        val display = viewFinder.display
        val metrics = DisplayMetrics().also { display.getMetrics(it) }
        val preview = Preview.Builder()
            .setTargetRotation(display.rotation)
            .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
            .build()
            .apply {
                previewSurfaceProvider = viewFinder.previewSurfaceProvider
//                setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
        preview.previewSurfaceProvider = viewFinder.previewSurfaceProvider
        return preview
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun buildImageAnalysisUseCase(): ImageAnalysis {
        val display = viewFinder.display
        val metrics = DisplayMetrics().also { display.getMetrics(it) }
        val analysis = ImageAnalysis.Builder()
            .setTargetRotation(display.rotation)
            .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
            .setImageQueueDepth(10)
            .build()
        analysis.setAnalyzer(
            Executors.newSingleThreadExecutor(),
            ImageAnalysis.Analyzer { imageProxy ->
                Log.d("CameraFragment", "Image analysis result $imageProxy")
                imageProxy.use {
                    try {
//                        val bitmapImage = imageProxy.image?.toBitmap()
//                        val stream = ByteArrayOutputStream()
//                        bitmapImage.compress(Bitmap.CompressFormat.PNG, 90, stream)
//                        val image = imageProxy.toJpeg() //write the logic here to convert imageproxy to byteArray
//                        Log.d("CameraFragment", "Image ByteArray $image")
                    } catch (t: Throwable) {
                        Log.d("CameraFragment", "Error in getting img")
                    }
                }
                imageProxy.close()
            })
        return analysis
    }

    private fun setupTapForFocus(cameraControl: CameraControl) {
        viewFinder.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener true
            }

            val textureView = viewFinder.getChildAt(0) as? TextureView
                ?: return@setOnTouchListener true
//            val factory = TextureViewMeteringPointFactory(textureView)
//
//            val point = factory.createPoint(event.x, event.y)
//            val action = FocusMeteringAction.Builder.from(point).build()
//            cameraControl.startFocusAndMetering(action)
            return@setOnTouchListener true
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                init()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
                //Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }


}