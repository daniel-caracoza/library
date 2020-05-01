package com.example.library

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.library.favorites.FavoritesActivity
import com.example.library.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity: AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var arSceneView: ArSceneView
    private lateinit var updateTextView: TextView
    private var isStarted: Boolean = false
    private var installRequested = false
    private lateinit var navigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        arSceneView = findViewById(R.id.sceneform_ar_scene_view)
        //to check for updates to the textView
        updateTextView = layoutInflater.inflate(R.layout.info_card, null) as TextView
        //listener to make updates, argument is the function called before frame is repainted
        arSceneView.scene.addOnUpdateListener(this::onUpdateFrame)
        navigationView = findViewById(R.id.bottom_navigation)
        navigationView.setOnNavigationItemSelectedListener(this)
    }

    //arbitrary function to show realtime update to ViewRenderable
    private fun getCurrentTime(): String {
        val current = LocalDateTime.now()
        val format = DateTimeFormatter.ofPattern("HH:mm:ss")
        return current.format(format)
    }
    //called everytime the frame is about to update
    private fun onUpdateFrame(frameTime: FrameTime){
        updateTextView.text = getCurrentTime()
    }

    private fun addObject(parse: Uri){
        //val frame = arFragment.arSceneView.arFrame
        val frame = arSceneView.arFrame
        val point = getScreenCenter()
        if(frame != null){
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for(hit in hits){
                val trackable = hit.trackable
                if(trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)){
                    //placeObject(hit.createAnchor(), parse)
                    placeInfoCard(hit.createAnchor())
                }
            }
        }
    }

    private fun placeInfoCard(createAnchor: Anchor){
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
        start()
    }

    private fun initializeSession() {
        if(ContextCompat.checkSelfPermission(this, "android.permission.CAMERA")
        == PackageManager.PERMISSION_GRANTED){
            var sessionException: UnavailableException? = null
            try {
                if(requestInstall())
                    return
                val session: Session = Session(this)
                val config:Config = session.config
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                session.configure(config)
                arSceneView.setupSession(session)
                return
            } catch(e: UnavailableException) {
                sessionException = e
                println(sessionException)
            }
        }
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

    private fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        try {
            arSceneView.resume()
        } catch (ex: CameraNotAvailableException) {
            println(ex.message)
        }
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
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0,0)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navigationView.postDelayed({
            when(item.itemId) {
                R.id.account -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.cameraView -> startActivity(Intent(this, MainActivity::class.java))
                R.id.favoriteList -> startActivity(Intent(this, FavoritesActivity::class.java))
            }
            finish()
        }, 300)
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
