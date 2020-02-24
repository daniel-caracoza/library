package com.example.library


import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Switch
import android.widget.TextView
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.FORMAT_ALL_FORMATS
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.TYPE_ISBN
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.fragment_camera_view_fragment.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.widget.CompoundButton
import com.google.ar.core.Session
import com.google.ar.core.SharedCamera
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.ux.ArFragment
import java.util.*


class CameraView_fragment : Fragment() {

    private lateinit var cameraView: TextureView
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var textShow: TextView
    private lateinit var parent: ViewParent
    private lateinit var switch: Switch
    //private lateinit var arSceneView: ArSceneView
    var switchNum = 2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera_view_fragment, container, false)

        cameraView = view.findViewById(R.id.view_finder)
        //arSceneView = view.findViewById(R.id.ar_scene_view)
        textShow = view.findViewById(R.id.text)
        switch = view.findViewById(R.id.barcodeSwitch)
        switch.setOnCheckedChangeListener{ _ , isChecked ->
            this.switchNum = if (isChecked) 1 else 2
        }
        parent = cameraView.parent

        cameraView.post {
            setUpCameraX()
        }

        return view
    }

    private fun setUpCameraX() {
        CameraX.unbindAll()

        val displayMetrics = DisplayMetrics().also { cameraView.display.getRealMetrics(it) }
        val screenSize = Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
        val aspectRatio = AspectRatio.RATIO_16_9
        val rotation = cameraView.display.rotation
        val session = Session(context, EnumSet.of(Session.Feature.SHARED_CAMERA))
        val arCam = session.sharedCamera

        val previewUseCase = buildPreviewUseCase(screenSize, aspectRatio, rotation)
        val analysUseCase = buildImageAnalysisUseCase(aspectRatio, rotation)

        CameraX.bindToLifecycle(this, previewUseCase, analysUseCase)
    }

    fun buildPreviewUseCase(screenSize: Size, aspectRatio: AspectRatio, rotation: Int): Preview {
        val previewConfig = PreviewConfig.Builder()
            .setTargetRotation(rotation)
            .setTargetResolution(Size(screenSize.width, screenSize.height))
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
                if(this@CameraView_fragment.switchNum == 2) {
                    val optionsB = FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(
                            FORMAT_ALL_FORMATS
                        ).build()
                    val barcodeDetector =
                        FirebaseVision.getInstance().getVisionBarcodeDetector(optionsB)
                    barcodeDetector.detectInImage(image).addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            Log.d("ISBN", rawValue.toString())
                            this@CameraView_fragment.text.text = rawValue
                        }
                    }
                }
                val options = FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.7f)
                    .build()
                val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler(options)
                labeler.processImage(image)
                    .addOnSuccessListener { labels ->
                        for (label in labels) {
                            val text = label.text
                            Log.d("Label", text)
                            if (text == "Poster"){
                                val detector = FirebaseVision.getInstance()
                                    .onDeviceTextRecognizer
                                detector.processImage(image)
                                    .addOnSuccessListener { firebaseVisionText: FirebaseVisionText ->
                                        // Task completed successfully
                                        val textar = mutableSetOf<String>()
                                        val resultText = firebaseVisionText.textBlocks
                                        val bookMap = mutableMapOf<Int, String>()
                                        for (block in resultText) {
                                            //val lines = block.lines
                                            //for(line in lines){
                                                val box = block.boundingBox?.height()
                                                val blockText = block.text
                                                var conf: Float = 0.0 as Float
                                                if(block.confidence != null){
                                                    conf = block.confidence as Float
                                                }
                                                if(box != null && conf > .5){
                                                    bookMap.put(box, blockText)
                                                }
                                                //Log.d("MLApp", "Text: $blockText")
                                            //}
                                            //textar.add(blockText)
                                            //var testst = textar.joinToString(" ")
                                            //this@CameraView_fragment.text.text = testst
                                        }
                                        val sortedBookMap = bookMap.toSortedMap()
                                        var valueOne = sortedBookMap.lastKey()
                                        val stringOne = sortedBookMap.get(valueOne)
                                        sortedBookMap.remove(valueOne)
                                        valueOne = sortedBookMap.lastKey()
                                        val stringTwo = sortedBookMap.get(valueOne)
                                        this@CameraView_fragment.text.text = stringOne + "\n" + stringTwo
                                        Log.d("BookStrings: ", stringOne)
                                    }
                                    .addOnFailureListener { e ->
                                        // Task failed with an exception
                                        //                  // ...
                                    }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        // ...
                    }

            }
        }
    }
}

