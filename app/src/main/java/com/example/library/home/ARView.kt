package com.example.library.home

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.example.library.R
import com.example.library.R.layout.boxlayout
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

class ARView : AppCompatActivity() {

    private lateinit var sceneView : ArSceneView
    private lateinit var textBox: TextView
    private lateinit var boxRenderable : ViewRenderable
    private var started = false
    private var boxPlaced = false
    private var loadingFinished = false
    private var installRequested = false
    private val imageAnalyzer = ImageAnalyzer()
    private var detector: FirebaseVisionTextRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a_r_scene_view)
        sceneView = findViewById(R.id.ar_scene_view)
        val box = ViewRenderable.builder().setView(this, boxlayout).build()
        CompletableFuture.allOf(box).handle { t, throwable ->
            if(throwable != null){
                Utils.displayError(this, "Unable to load renderable", throwable)
            }
            try{
                boxRenderable = box.get()
                loadingFinished = true
                textBox = boxRenderable.view.findViewById(R.id.topText)
                sceneView.scene.addOnUpdateListener(this::onIncreaseTime)
            } catch (e : Exception){
                Utils.displayError(this, "Unable to render", e)
            }
        }
        sceneView.scene.addOnUpdateListener(this::onUpdateFrame)
        sceneView.planeRenderer.isVisible = false
        gestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener(){
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val frame = sceneView.arFrame
                if(frame != null){
                    if(!boxPlaced && tryPlaceBox(e, frame)){
                        boxPlaced = true
                    }
                }
                return true
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return false
            }
        })
        sceneView.scene.setOnTouchListener{ hitTestReult: HitTestResult, event : MotionEvent->
            if(!boxPlaced){
                return@setOnTouchListener gestureDetector.onTouchEvent(event)
            }
            return@setOnTouchListener false
        }
    }

    private fun getCurrentTime(): String{
        val current = LocalDateTime.now()
        val format = DateTimeFormatter.ofPattern("HH:mm:ss")
        return current.format(format)
    }

    private fun onIncreaseTime(frameTime: FrameTime){
        //textBox.text = getCurrentTime()
        val image = sceneView.arFrame?.acquireCameraImage()
        if(image != null){
            val camId = getCameraId(this, CameraCharacteristics.LENS_FACING_BACK)
            val rotation =getRotationCompensation(camId, this@ARView as Activity, this)
            val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)
            val result = detector.processImage(firebaseVisionImage)
                    .addOnSuccessListener { firebaseVisionText ->
                        val resultText = firebaseVisionText.text
                        for (block in firebaseVisionText.textBlocks) {
                            val blockText = block.text
                            val blockConfidence = block.confidence
                            val blockLanguages = block.recognizedLanguages
                            val blockCornerPoints = block.cornerPoints
                            val blockFrame = block.boundingBox
                            for (line in block.lines) {
                                val lineText = line.text
                                textBox.text = lineText
                                val lineConfidence = line.confidence
                                val lineLanguages = line.recognizedLanguages
                                val lineCornerPoints = line.cornerPoints
                                val lineFrame = line.boundingBox
                                for (element in line.elements) {
                                    val elementText = element.text
                                    val elementConfidence = element.confidence
                                    val elementLanguages = element.recognizedLanguages
                                    val elementCornerPoints = element.cornerPoints
                                    val elementFrame = element.boundingBox
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

    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }
    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(cameraId: String, activity: Activity, context: Context): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
                Log.e("cam", "Bad rotation value: $rotationCompensation")
            }
        }
        return result
    }

    private class ImageAnalyzer : ImageAnalysis.Analyzer {
        private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
        }

        override fun analyze(imageProxy: ImageProxy?, degrees: Int) {
            val mediaImage = imageProxy?.image
            val imageRotation = degreesToFirebaseRotation(degrees)
            if (mediaImage != null) {
                val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                // Pass image to an ML Kit Vision API
                // ...
            }
        }
    }

   private fun onUpdateFrame(frameTime : FrameTime){
        val frame = sceneView.arFrame
       if(frame != null){
           val var3 = frame.getUpdatedTrackables(Plane::class.java).iterator()
           while(var3.hasNext()){
               val plane = var3.next() as Plane
               if(plane.trackingState == TrackingState.TRACKING && !boxPlaced){
                   val iterableAnchor = frame.updatedAnchors.iterator()
                   if(!iterableAnchor.hasNext()){
                       val hitTest = frame.hitTest(frame.screenCenter().x, frame.screenCenter().y)
                       val hitTestIterator = hitTest.iterator()
                       if(hitTestIterator.hasNext()){
                           val hitResult = hitTestIterator.next()
                           val modelAnchor = plane.createAnchor(hitResult.hitPose)
                           val anchorNode = AnchorNode(modelAnchor)
                           anchorNode.setParent(sceneView.scene)
                           val box = Node()
                           box.setParent(anchorNode)
                           box.renderable = boxRenderable
                           box.worldPosition = Vector3(modelAnchor.pose.tx(), modelAnchor.pose.compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(), modelAnchor.pose.tz())
                       }
                   }
                   boxPlaced = true
                   textBox.text = getCurrentTime()
               }
           }

       }
   }

    private fun Frame.screenCenter(): Vector3{
        val vw = findViewById<View>(android.R.id.content)
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }

    override fun onResume() {
        super.onResume()
        if(sceneView.session == null) {
            startSession()
        }
        start()
    }

    private fun startSession(){
        if(ContextCompat.checkSelfPermission(this, "android.permission.CAMERA")
        == PackageManager.PERMISSION_GRANTED){
            try{
                if(installRequested)
                    return
                val session = Session(this)
                val config : Config = session.config
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                session.configure(config)
                sceneView.setupSession(session)
                return
            } catch(e : UnavailableException){
                Utils.handleSessionException(this, e)
            }
        }
    }

    fun getCameraId(context: Context, facing: Int): String {
        val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager

        return manager.cameraIdList.first {
            manager
                    .getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == facing
        }
    }

    private fun start(){
        if(started)
            return
        started = true
        try{
            sceneView.resume()
        } catch (e : CameraNotAvailableException){
            Log.e("Camera not available", "")
        }
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        sceneView.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(!Utils.hasCameraPermission(this)){
            if(!Utils.shouldSHowREquestPermissionRational(this)){
                Utils.launchPermissionSettings(this)
            } else {
                Toast.makeText(this, "Camera permission is needede to run this application", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }


    private fun onSingleTap(tap: MotionEvent){
        if(!loadingFinished){
            return
        }
        var frame = sceneView.arFrame
        if(frame != null){
            if(!boxPlaced && tryPlaceBox(tap, frame)){
                boxPlaced = true
            }
        }
    }

    private fun tryPlaceBox(tap: MotionEvent, frame: Frame): Boolean{
        if(tap != null && frame.camera.trackingState == TrackingState.TRACKING){
            for(hit in frame.hitTest(tap)){
                var trackable = hit.trackable
                if(trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)){
                    var anchor = hit.createAnchor()
                    var anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(sceneView.scene)
                    createBox()
                    return true
                }
            }
        }
        return false
    }

    private fun createBox(): Node{
        val base = Node()
        val box = Node()
        box.setParent(base)
        box.renderable = boxRenderable
        box.localPosition = Vector3(0.0f, 0.25f, 0.0f)
        return base
    }

    private fun showLoadingMessage(){
        if(!loadingFinished){
            Toast.makeText(this, "Searching for surfaces", Toast.LENGTH_LONG).show()
        }
    }
}
