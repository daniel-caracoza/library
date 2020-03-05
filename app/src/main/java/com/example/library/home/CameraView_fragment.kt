package com.example.library.home


import android.graphics.Matrix
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.library.home.CameraView_fragmentDirections
import com.example.library.R
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.fragment_camera_view_fragment.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraView_fragment : Fragment(), View.OnClickListener {

    private lateinit var cameraView: TextureView
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var textShow: TextView
    private lateinit var parent: ViewParent
    private lateinit var extractedText: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera_view_fragment, container, false)

        cameraView = view.findViewById(R.id.view_finder)
        textShow = view.findViewById(R.id.text)
        parent = cameraView.parent

        cameraView.post {
            setUpCameraX()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.bookButton).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.bookButton -> {
                val action =
                    CameraView_fragmentDirections.actionCameraViewToBookInformation(
                        extractedText
                    )
                v!!.findNavController().navigate(action)

            }
        }
    }


    private fun setUpCameraX() {
        CameraX.unbindAll()

        val displayMetrics = DisplayMetrics().also { cameraView.display.getRealMetrics(it) }
        val screenSize = Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
        //val aspectRatio = Rational(displayMetrics.widthPixels, displayMetrics.heightPixels)
        val aspectRatio = AspectRatio.RATIO_16_9
        val rotation = cameraView.display.rotation

        val previewUseCase = buildPreviewUseCase(screenSize, aspectRatio, rotation)
        val analysUseCase = buildImageAnalysisUseCase(aspectRatio, rotation)

        CameraX.bindToLifecycle(this, previewUseCase, analysUseCase)
    }

    fun buildPreviewUseCase(screenSize: Size, aspectRatio: AspectRatio, rotation: Int): Preview {
        val previewConfig = PreviewConfig.Builder()
            .setTargetRotation(rotation)
            .setTargetResolution(Size(640, 480))
            .setLensFacing(CameraX.LensFacing.BACK)
            .build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener { previewOutput ->
            //view_finder.surfaceTexture = previewOutput.surfaceTexture
            val p = parent as ViewGroup
            p.removeView(cameraView)
            cameraView.surfaceTexture = previewOutput.surfaceTexture
            p.addView(cameraView, 0)
            updateTransform()
        }

        return preview
    }

    fun buildImageAnalysisUseCase(aspectRatio: AspectRatio, rotation: Int): ImageAnalysis {
        val analysisConfig = ImageAnalysisConfig.Builder()
            .setTargetRotation(rotation)
            .setTargetResolution(Size(640, 480))
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            .build()

        val analysis = ImageAnalysis(analysisConfig)
        analysis.setAnalyzer(executor, MLAnalyzer())


        return analysis
    }

    private fun updateTransform() {
        val matrix = Matrix()

        val centerX = cameraView.width / 2f
        val centerY = cameraView.height / 2f

        val rotationDegrees = when (cameraView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        cameraView.setTransform(matrix)
    }

    private inner class MLAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L
        private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
        }

        /**
         * Helper extension function used to extract a byte array from an
         * image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()
            // Calculate the average luma no more often than every second
            if (currentTimestamp - lastAnalyzedTimestamp >=
                TimeUnit.SECONDS.toMillis(1)
            ) {
                // Since format in ImageAnalysis is YUV, image.planes[0]
                // contains the Y (luminance) plane
                val y = image.planes[0]
                val u = image.planes[1]
                val v = image.planes[2]
                val Yb = y.buffer.remaining()
                val Ub = u.buffer.remaining()
                val Vb = v.buffer.remaining()
                val data = ByteArray(Yb + Ub + Vb)
                y.buffer.get(data, 0, Yb)
                u.buffer.get(data, Yb, Ub)
                v.buffer.get(data, Yb + Ub, Vb)
                val buffer = image.planes[0].buffer
                // Extract image data from callback object
                //val data = buffer.toByteArray()
                // Convert the data into an array of pixel values
                val pixels = data.map { it.toInt() and 0xFF }
                //              // Compute average luminance for the image
                val luma = pixels.average()
                //          // Log the new luma value
                Log.d("CameraXApp", "Average luminosity: $luma")
                //      // Update timestamp of last analyzed frame
                lastAnalyzedTimestamp = currentTimestamp
                val metadata = FirebaseVisionImageMetadata.Builder()
                    .setWidth(image.width) // 480x360 is typically sufficient for
                    .setHeight(image.height) // image recognition
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
                    .setRotation(degreesToFirebaseRotation(rotationDegrees))
                    .build()
                val image = FirebaseVisionImage.fromByteArray(data, metadata)
                val detector = FirebaseVision.getInstance()
                    .onDeviceTextRecognizer
                detector.processImage(image)
                    .addOnSuccessListener { firebaseVisionText: FirebaseVisionText ->
                        //          // Task completed successfully
                        // ...
                        val resultText = firebaseVisionText.textBlocks
                        for (block in resultText) {
                            val textar = mutableSetOf<String>();
                            val blockText = block.text
                            Log.d("MLApp", "Text: $blockText")
                            textar.add(blockText)
                            var testst = textar.joinToString(" ")
                            text.text = testst
                            extractedText = testst
                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        //                  // ...
                    }
            }
        }
    }
}

