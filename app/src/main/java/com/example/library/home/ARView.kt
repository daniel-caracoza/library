package com.example.library.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.library.R
import com.example.library.R.layout.boxlayout
import com.example.library.favorites.FavoritesActivity
import com.example.library.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import kotlinx.android.synthetic.main.activity_a_r_scene_view.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

class ARView : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener{

    private lateinit var sceneView : ArSceneView
    private lateinit var textBox: TextView
    private lateinit var boxRenderable : ViewRenderable
    private var started = false
    var returnText = ""
    private var boxPlaced = false
    private var loadingFinished = false
    private var installRequested = false
    private lateinit var frameLayout: FrameLayout
    private lateinit var navigationView: BottomNavigationView
    private var time = LocalDateTime.MIN
    private var detector: FirebaseVisionTextRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        //Inflation and set up of the activity
        super.onCreate(savedInstanceState)
        val constraintLayout:ConstraintLayout = layoutInflater.inflate(R.layout.activity_a_r_scene_view, null) as ConstraintLayout
        frameLayout = constraintLayout.findViewById(R.id.ar_frame_layout)
        sceneView = frameLayout.findViewById(R.id.ar_scene_view)
        navigationView = constraintLayout.findViewById(R.id.bottom_navigation)
        navigationView.setOnNavigationItemSelectedListener(this)
        super.setContentView(constraintLayout)
        setSupportActionBar(toolbar)
        //Initializing the renderable text box
        val box = ViewRenderable.builder().setView(this, boxlayout).build()
        CompletableFuture.allOf(box).handle { t, throwable ->
            if(throwable != null){
                Utils.displayError(this, "Unable to load renderable", throwable)
            }
            try{
                //loading renderable text box
                boxRenderable = box.get()
                loadingFinished = true
                textBox = boxRenderable.view.findViewById(R.id.topText)
                //adding onUpdateListener to update the text on the text box
                sceneView.scene.addOnUpdateListener(this::onIncreaseTime)
            } catch (e : Exception){
                Utils.displayError(this, "Unable to render", e)
            }
        }
        //onUpdateListener for the ARSceneView to place the box
        sceneView.scene.addOnUpdateListener(this::placeBox)
        //Turn the dots on or off for finding planes
        sceneView.planeRenderer.isVisible = true
        //Set up of the GestureDetector which places the box when a plane is found
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
        //Box gets automatically placed when a plane is found, no tapping required
        sceneView.scene.setOnTouchListener{ hitTestReult: HitTestResult, event : MotionEvent->
            if(!boxPlaced){
                return@setOnTouchListener gestureDetector.onTouchEvent(event)
            }
            return@setOnTouchListener false
        }
    }

    //Unneeded function for testing
    private fun getCurrentTime(): String{
        val current = LocalDateTime.now()
        val format = DateTimeFormatter.ofPattern("HH:mm:ss")
        return current.format(format)
    }

    private fun onIncreaseTime(frameTime: FrameTime){
        //Only update the text box every second
        val testTime = LocalDateTime.now()
        if(time.plusSeconds(1).isBefore(testTime)) {
            time = testTime
            //If the camera image is not null get the frame from the ARSceneView
            val image = sceneView.arFrame?.acquireCameraImage()
            if (image != null) {
                //There's no good way to get the cameraID from the ARSceneView,
                //that's one of the things that the ARFragment does.
                //So I just automatically get the back lens for the camera that is being used
                val camId = getCameraId(this, CameraCharacteristics.LENS_FACING_BACK)
                //Firebase needs to know the rotation of the phone, but wants it in it's special way
                val rotation = getRotationCompensation(camId, this@ARView as Activity, this)
                val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)
                //Once we have the image as a Firebase image we can close the image so it's not taking up space
                image.close()
                //This is the basic Firebase text extraction code given by Google
                val result = detector.processImage(firebaseVisionImage)
                        .addOnSuccessListener { firebaseVisionText ->
                            val resultText = firebaseVisionText.text
                            for (block in firebaseVisionText.textBlocks) {
                                val blockText = block.text
                                textBox.text = blockText
                                returnText = blockText
                                val blockConfidence = block.confidence
                                val blockLanguages = block.recognizedLanguages
                                for (line in block.lines) {
                                    val lineText = line.text
                                    val lineConfidence = line.confidence
                                    val lineLanguages = line.recognizedLanguages
                                    for (element in line.elements) {
                                        val elementText = element.text
                                        val elementConfidence = element.confidence
                                        val elementLanguages = element.recognizedLanguages
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

    //This is for the Firebase rotations
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

    //This is the automatic box placer
   private fun placeBox(frameTime : FrameTime){
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
                   textBox.text = "Library++"
               }
           }

       }
   }

    //finds the screen center
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

    //checks for permissions on start
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

    //function to get the camera id from some facing camera
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

    //box node maker for placing
    private fun tryPlaceBox(tap: MotionEvent, frame: Frame): Boolean{
        if(frame.camera.trackingState == TrackingState.TRACKING){
            for(hit in frame.hitTest(tap)){
                val trackable = hit.trackable
                if(trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)){
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(sceneView.scene)
                    createBox()
                    return true
                }
            }
        }
        return false
    }

    //Create renderable
    private fun createBox(): Node{
        val base = Node()
        val box = Node()
        box.setParent(base)
        box.renderable = boxRenderable
        box.localPosition = Vector3(0.0f, 0.25f, 0.0f)
        return base
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navigationView.postDelayed({
            when(item.itemId) {
                R.id.account -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.cameraView -> startActivity(Intent(this, ARView::class.java))
                R.id.favoriteList -> startActivity(Intent(this, FavoritesActivity::class.java))
            }
            finish()
        }, 100)
        return true
    }

    private fun updateNavigationBarState(){
        val actionId = R.id.cameraView
        selectBottomNavigationBarItem(actionId)
    }

    private fun selectBottomNavigationBarItem(itemId: Int){
        val menuItem = navigationView.menu.findItem(itemId)
        menuItem.isChecked = true
    }
}
