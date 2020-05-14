package com.example.library.home

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.library.R
import com.example.library.favorites.FavoritesActivity
import com.example.library.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.*
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.PlaneDiscoveryController
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity() : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener,
Scene.OnPeekTouchListener, Scene.OnUpdateListener, Node.OnTapListener {

    private lateinit var arSceneView: ArSceneView
    private lateinit var updateTextView: TextView
    private var installRequested = false
    private val cameraRequestCode: Int = 1
    private var renderableExists = false
    private lateinit var navigationView: BottomNavigationView
    private lateinit var frameLayout: FrameLayout
    private lateinit var gestureDetector: GestureDetector
    private lateinit var planeDiscoveryController: PlaneDiscoveryController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        gestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener(){
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                onSingleTap(e)
                return true
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }
        })
    }

    override fun setContentView(layoutResID: Int) {
        val constraintLayout:ConstraintLayout = layoutInflater.inflate(R.layout.activity_main, null) as ConstraintLayout
        frameLayout = constraintLayout.findViewById(R.id.ar_frame_layout)
        arSceneView = frameLayout.findViewById(R.id.sceneform_ar_scene_view)
        arSceneView.scene.addOnUpdateListener(this)
        arSceneView.scene.addOnPeekTouchListener(this)

        navigationView = constraintLayout.findViewById(R.id.bottom_navigation)
        updateTextView = layoutInflater.inflate(R.layout.info_card, null) as TextView
        navigationView.setOnNavigationItemSelectedListener(this)

        val instructionsView: View = layoutInflater.inflate(R.layout.sceneform_plane_discovery_layout, frameLayout, false)
        planeDiscoveryController = PlaneDiscoveryController(instructionsView)
        frameLayout.addView(instructionsView)
        super.setContentView(constraintLayout)
    }

    //arbitrary function to show realtime update to ViewRenderable
    private fun getCurrentTime(): String {
        val current = LocalDateTime.now()
        val format = DateTimeFormatter.ofPattern("HH:mm:ss")
        return current.format(format)
    }

    //called everytime the frame is about to update
    override fun onUpdate(frameTime: FrameTime){
        val frame = arSceneView.arFrame ?: return
        val planes = frame.getUpdatedTrackables(Plane::class.java)
        for(plane in planes){
            if(plane.trackingState == TrackingState.TRACKING){
                planeDiscoveryController.hide()
            }
        }
        updateTextView.text = getCurrentTime()
    }

    override fun onPeekTouch(hitTestResult: HitTestResult?, motionEvent: MotionEvent?) {
        if(hitTestResult!!.node == null){
            gestureDetector.onTouchEvent(motionEvent)
        }
    }

    //invoked from a tap on the plane
    private fun onSingleTap(tap:MotionEvent?){
        val frame = arSceneView.arFrame
        if(frame != null && !renderableExists){
            if(tap != null && frame.camera.trackingState == TrackingState.TRACKING){
                val hits = frame.hitTest(tap)
                for(hit:HitResult in hits){
                    val trackable = hit.trackable
                    if(trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)){
                        initializeInfoCard(hit.createAnchor())
                        arSceneView.planeRenderer.isVisible = false
                        break
                    }
                }
            }
            renderableExists = true
        }
    }

    private fun initializeInfoCard(createAnchor: Anchor){
        ViewRenderable.builder()
            .setView(this, R.layout.info_card)
            .build()
            .thenAccept {
                it.isShadowCaster = false
                it.isShadowReceiver = false
                addCardToScene(createAnchor, it)
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message)
                    .setTitle("error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    private fun addCardToScene(createAnchor: Anchor, renderable: ViewRenderable){
        val anchorNode = AnchorNode(createAnchor)
        val node = Node()
        updateTextView = renderable.view as TextView
        updateTextView.text = getCurrentTime()
        node.renderable = renderable
        node.setParent(anchorNode)
        arSceneView.scene.addChild(anchorNode)
    }


    private fun getScreenCenter() :android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.width / 2 , vw.height / 2)
    }

    override fun onResume() {
        super.onResume()
        if(arSceneView.session == null){
            initializeSession()
        }
        try {
            arSceneView.resume()
        } catch (ex:CameraNotAvailableException){println(ex.message)}
    }

    private fun initializeSession() {
        if(ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), cameraRequestCode)
        }
        try {
            if(requestInstall())
                return
            val session = Session(this)
            val config:Config = session.config
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            session.configure(config)
            arSceneView.setupSession(session)
            return
        } catch(e: UnavailableException) {
            println(e.message)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            cameraRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(
                        this,
                        "Camera permission is needed to run this application!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    @Throws(UnavailableException::class)
    private fun requestInstall(): Boolean {
        when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
            InstallStatus.INSTALL_REQUESTED -> {
                installRequested = true
                return true
            }
            InstallStatus.INSTALLED -> {
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.view_toggle_menu -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onStart() {
        super.onStart()
        updateNavigationBarState()
        planeDiscoveryController.show()
    }

    override fun onPause() {
        super.onPause()
        arSceneView.pause()
        overridePendingTransition(0,0)
        planeDiscoveryController.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSceneView.destroy()
        planeDiscoveryController.hide()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navigationView.postDelayed({
            when(item.itemId) {
                R.id.account -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.cameraView -> startActivity(Intent(this, MainActivity::class.java))
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

    override fun onTap(p0: HitTestResult?, p1: MotionEvent?) {
        TODO("Not yet implemented")
    }
}
