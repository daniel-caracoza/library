package com.example.library.home

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.library.R
import com.example.library.R.layout.boxlayout
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class ARView : AppCompatActivity() {

    private lateinit var sceneView : ArSceneView
    private lateinit var textBox: TextView
    private lateinit var boxRenderable : ViewRenderable
    private var started = false
    private var boxPlaced = false
    private var loadingFinished = false
    private var installRequested = false
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
        gestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener(){
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                var frame = sceneView.arFrame
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
        textBox.text = getCurrentTime()
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
        var base = Node()
        var box = Node()
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
