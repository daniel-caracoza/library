package com.example.library.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.library.ApplicationRepository
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.Favorite
import com.example.library.favorites.FavoritesActivity
import com.example.library.network.GoogleBook
import com.example.library.network.Items
import com.example.library.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.PlaneDiscoveryController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class MainActivity() : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener,
Scene.OnPeekTouchListener, Scene.OnUpdateListener {

    private lateinit var arSceneView: ArSceneView
    private lateinit var updateTextView: TextView
    private var installRequested = false
    private val cameraRequestCode: Int = 1
    private var renderableExists = false
    private lateinit var repository: ApplicationRepository
    private lateinit var navigationView: BottomNavigationView
    private lateinit var frameLayout: FrameLayout
    private lateinit var gestureDetector: GestureDetector
    private lateinit var planeDiscoveryController: PlaneDiscoveryController
    private lateinit var bookModelRenderable: ModelRenderable
    private lateinit var coinModelRenderable: ModelRenderable
    private lateinit var infoModelRenderable: ModelRenderable
    private var coinNode: Node? = null
    private var bookNode: Node? = null
    private var authorNode: Node? = null
    private var favorite: Favorite? = null
    private var googleBookItems: Items? = null
    private var googleBook: Items? = null
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val application = requireNotNull(this).application
        val dataSource = AppDatabase.getInstance(application)
        repository = ApplicationRepository(dataSource)
        gestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener(){
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                onSingleTap(e)
                return true
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }
        })


        // Build all the models.
        val bookStage = ModelRenderable.builder().setSource(this, Uri.parse("Book.sfb")).build()
        val coinStage = ModelRenderable.builder().setSource(this, Uri.parse("Coin.sfb")).build()
        val modelStage = ModelRenderable.builder().setSource(this, Uri.parse("Model.sfb")).build()

        CompletableFuture.allOf(
            bookStage,
            coinStage,
            modelStage
        )
            .handle<Any?> { notUsed: Void?, throwable: Throwable? ->
                // When you build a Renderable, Sceneform loads its resources in the background while
                // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                // before calling get().
                try {
                    bookModelRenderable = bookStage.get()
                    coinModelRenderable = coinStage.get()
                    infoModelRenderable = modelStage.get()

                } catch (ex: InterruptedException) {
                    println(ex.message)
                } catch (ex: ExecutionException) {
                    println(ex.message)
                }
                null
            }
        /*
            Hardcoded api calls to fill model-views with information
         */
        initiateApiRequests()
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


    //called everytime the frame is about to update
    override fun onUpdate(frameTime: FrameTime){
        val frame = arSceneView.arFrame ?: return
        val planes = frame.getUpdatedTrackables(Plane::class.java)
        for(plane in planes){
            if(plane.trackingState == TrackingState.TRACKING){
                planeDiscoveryController.hide()
            }
        }
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
                        val anchor = hit.createAnchor()
                        val anchorNode = AnchorNode(anchor)
                        anchorNode.setParent(arSceneView.scene)
                        val bookView = createBookView()
                        anchorNode.addChild(bookView)
                        arSceneView.planeRenderer.isVisible = false
                        break
                    }
                }
            }
            renderableExists = !renderableExists
        }
    }

    private fun createBookView(): Node {
        val base = Node()
        coinNode = PriceNode(this, googleBook!!.items[0])
        coinNode!!.renderable = coinModelRenderable
        coinNode!!.setParent(base)
        coinNode!!.localPosition = Vector3(-0.2f, 0.1f, 0.0f)

        bookNode = BookNode(this, googleBook!!.items[0])
        bookNode!!.renderable = bookModelRenderable
        bookNode!!.setParent(base)
        bookNode!!.localPosition = Vector3(0.0f, 0.1f, 0.0f)

        authorNode = AuthorNode(this, googleBook!!.items[0], googleBookItems!!)
        authorNode!!.renderable = infoModelRenderable
        authorNode!!.setParent(base)
        authorNode!!.localPosition = Vector3(0.2f, 0.1f, 0.0f)

        return base
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

    //set functionality for the selected items in the toolbar menu
    override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
        //set response to toggling option menu here!
        R.id.toggle_isbn_search, R.id.toggle_title_search -> {
            item.isChecked = !(item.isChecked)
            true
        }

        R.id.mark_favorite -> {
            if(favorite != null){
                uiScope.launch {
                        withContext(Dispatchers.IO){
                            repository.addFavoriteAsync(favorite!!)
                        }
                    }
                }
            true
        }

        R.id.check_coin_view -> {
            //toggle coin node
            coinNode!!.isEnabled = !(coinNode!!.isEnabled)
            item.isChecked = !(item.isChecked)
            true
        }

        R.id.check_info_view -> {
            //toggle info node
            authorNode!!.isEnabled = !(authorNode!!.isEnabled)
            item.isChecked = !(item.isChecked)
            true
        }

        R.id.check_book_view -> {
            //toggle book node
            bookNode!!.isEnabled = !(bookNode!!.isEnabled)
            item.isChecked = !(item.isChecked)
            true
        }

        else -> super.onOptionsItemSelected(item)

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
        job.cancel()
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

    private fun initiateApiRequests(){
            val isbn = "9780451524935"
            val author = "inauthor:george+orwell"
            googleBook = runBlocking {repository.googleBooksSearchRequest("isbn:${isbn}")}
            googleBookItems = runBlocking {repository.googleBooksSearchRequest(author)}
    }

}
