package com.example.library.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.library.ApplicationRepository
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.Favorite
import com.example.library.favorites.FavoritesActivity
import com.example.library.network.GoodreadsResponse
import com.example.library.network.GoogleBook
import com.example.library.network.Items
import com.example.library.settings.SettingsActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import kotlinx.android.synthetic.main.dialog_box.*
import java.lang.AssertionError
import java.lang.Math.abs
import java.time.LocalDateTime

class MainActivity() : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener,
Scene.OnPeekTouchListener, Scene.OnUpdateListener, DialogBox.NoticeDialogListener {

    private lateinit var sceneView: ArSceneView
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
    private lateinit var firebaseViewRenderable: ViewRenderable
    private lateinit var textBox: TextView
    private lateinit var switch:Switch
    private var boxPlaced = false
    var returnText = ""
    private var coinNode: Node? = null
    private var bookNode: Node? = null
    private var authorNode: Node? = null
    private var base:Node? = null
    private var time = LocalDateTime.MIN
    private var detector: FirebaseVisionTextRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
    private var anchorNode: AnchorNode? = null
    private var box: Node = Node()

    private var googleBook:GoogleBook? = null
    val _googleBook: GoogleBook
        get(){
            return googleBook ?: throw AssertionError("Set to null")
        }

    private var goodreadsResponse: GoodreadsResponse? = null
    val _goodreadsResponse: GoodreadsResponse
        get(){
            return goodreadsResponse ?: throw AssertionError("Set to null")
        }

    private val job = Job()
    private var refJob: Job? = null
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private var bookViewCreated = false
    private var requestedBookView = false
    //Dialog Box
    private lateinit var textBox1: TextView
    private lateinit var textBox2 : TextView
    private lateinit var okButton: Button
    //Sign in
    private var userId:Long = 0
    private lateinit var sharedPreferences: SharedPreferences

    val SWIPE_THRESHOLD:Float = 100f
    val SWIPE_VELOCITY_THRESHOLD:Float = 100f

    companion object {
        var returnText1 = ""
        var returnText2 = ""
        var barcode = false
        var returnBarcode = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val application = requireNotNull(this).application
        val dataSource = AppDatabase.getInstance(application)
        repository = ApplicationRepository(dataSource)
        //get user creds
        sharedPreferences = application.getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)
        userId = retrieveSignIn()

        // Build all the models.
        val bookStage = ModelRenderable.builder().setSource(this, Uri.parse("Book.sfb")).build()
        val coinStage = ModelRenderable.builder().setSource(this, Uri.parse("Coin.sfb")).build()
        val modelStage = ModelRenderable.builder().setSource(this, Uri.parse("Model.sfb")).build()
        val firebaseViewStage = ViewRenderable.builder().setView(this, R.layout.boxlayout).build()


        CompletableFuture.allOf(
            bookStage,
            coinStage,
            modelStage,
            firebaseViewStage
        )
            .handle<Any?> { notUsed: Void?, throwable: Throwable? ->
                // When you build a Renderable, Sceneform loads its resources in the background while
                // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                // before calling get().
                try {
                    bookModelRenderable = bookStage.get()
                    coinModelRenderable = coinStage.get()
                    infoModelRenderable = modelStage.get()
                    firebaseViewRenderable = firebaseViewStage.get()
                    sceneView.scene.addOnUpdateListener(this::onIncreaseTime)
                    textBox1 = firebaseViewRenderable.view.findViewById(R.id.topText)
                    textBox2 = firebaseViewRenderable.view.findViewById(R.id.bottomText)
                    textBox = firebaseViewRenderable.view.findViewById(R.id.topText)
                    okButton = firebaseViewRenderable.view.findViewById(R.id.OKbutton)
                    switch = firebaseViewRenderable.view.findViewById(R.id.bcode_switch)
                    okButton.setOnClickListener{
                        val dialog = DialogBox(returnText1, returnText2)
                        dialog.show(supportFragmentManager, "DialogBox")
                    }

                } catch (ex: InterruptedException) {
                    println(ex.message)
                } catch (ex: ExecutionException) {
                    println(ex.message)
                }
                null
            }
        sceneView.scene.addOnUpdateListener(this::placeBox)

        gestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener(){
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                //where the book view was initially created
                //onSingleTap(e)
                val frame = sceneView.arFrame
                if(frame != null){
                    if(!boxPlaced && tryPlaceBox(e, frame)){
                        boxPlaced = true
                    }
                }
                return true
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onFling(
                downEvent: MotionEvent?,
                moveEvent: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                var result = false
                val diffY = moveEvent!!.y - downEvent!!.y
                val diffX = moveEvent.x - downEvent.x

                if(kotlin.math.abs(diffX) > kotlin.math.abs(diffY)){
                    //right or left swipe
                    if(kotlin.math.abs(diffX) > SWIPE_THRESHOLD && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD){
                        if(diffX < 0){
                            //Swipe left
                            if(bookViewCreated){
                                anchorNode!!.removeChild(base)
                                Toast.makeText(this@MainActivity, "AR view removed, reloading scanner...", Toast.LENGTH_LONG).show()
                                bookViewCreated = false
                                sceneView.planeRenderer.isVisible = true
                            }
                        }
                    }
                }
                return super.onFling(downEvent, moveEvent, velocityX, velocityY)
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

    override fun setContentView(layoutResID: Int) {
        val constraintLayout:ConstraintLayout = layoutInflater.inflate(R.layout.activity_main, null) as ConstraintLayout
        frameLayout = constraintLayout.findViewById(R.id.ar_frame_layout)
        sceneView = frameLayout.findViewById(R.id.sceneform_ar_scene_view)
        //To update text on firebase view frame by frame
        sceneView.scene.addOnPeekTouchListener(this)
        sceneView.scene.addOnUpdateListener(this)
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
        val frame = sceneView.arFrame ?: return
        val planes = frame.getUpdatedTrackables(Plane::class.java)
        for(plane in planes){
            if(plane.trackingState == TrackingState.TRACKING){
                planeDiscoveryController.hide()
            }
        }
        if (refJob == null) return

        else if(refJob!!.isCompleted && !bookViewCreated && requestedBookView){
            createBookView()
            bookViewCreated = true
            requestedBookView = false
        }
    }

    private fun onIncreaseTime(frameTime: FrameTime){
        if(sceneView.arFrame == null) return
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
                val rotation = getRotationCompensation(camId, this@MainActivity as Activity, this)
                val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation)
                //Once we have the image as a Firebase image we can close the image so it's not taking up space
                image.close()
                if(switch.isChecked) {
                    barcode = true
                    val detector = FirebaseVision.getInstance()
                        .visionBarcodeDetector
                    val resultBC = detector.detectInImage(firebaseVisionImage)
                        .addOnSuccessListener { barcodes ->
                            // Task completed successfully
                            for (barcode in barcodes) {
                                val bounds = barcode.boundingBox
                                val corners = barcode.cornerPoints

                                val rawValue = barcode.rawValue

                                if(rawValue != null){
                                    returnBarcode = rawValue
                                }
                            }

                        }
                        .addOnFailureListener {
                            // Task failed with an exception
                            // ...
                        }
                }
                if(!switch.isChecked){
                    barcode = false
                }

                //This is the basic Firebase text extraction code given by Google
                val result = detector.processImage(firebaseVisionImage)
                    .addOnSuccessListener { firebaseVisionText ->
                        val resultText = firebaseVisionText.text
                        var text1Height = 0
                        var text2Height = 0
                        for (block in firebaseVisionText.textBlocks) {
                            val blockText = block.text
                            val blockConfidence = block.confidence
                            val blockLanguages = block.recognizedLanguages
                            for (line in block.lines) {
                                var height = line.boundingBox?.height()?.let { abs(it) }
                                val lineText = line.text
                                val lineConfidence = line.confidence
                                val lineLanguages = line.recognizedLanguages

                                textBox2.text = textBox1.text
                                textBox1.text = lineText
                                returnText1 = lineText
                                returnText2 = textBox2.text.toString()

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
    //This is the automatic box placer
    private fun placeBox(frameTime : FrameTime){
        val frame = sceneView.arFrame
        if(frame != null){
            val var3 = frame.getUpdatedTrackables(Plane::class.java).iterator()
            while(var3.hasNext()){
                val plane = var3.next() as Plane
                //check if tracking, if firebase box is there already or if bookView is there already
                if(plane.trackingState == TrackingState.TRACKING && !boxPlaced && !bookViewCreated){
                    val iterableAnchor = frame.updatedAnchors.iterator()
                    if(!iterableAnchor.hasNext()){
                        val hitTest = frame.hitTest(frame.screenCenter().x, frame.screenCenter().y)
                        val hitTestIterator = hitTest.iterator()
                        if(hitTestIterator.hasNext()){
                            val hitResult = hitTestIterator.next()
                            val modelAnchor = plane.createAnchor(hitResult.hitPose)
                            anchorNode = AnchorNode(modelAnchor)
                            anchorNode!!.setParent(sceneView.scene)
                            box.setParent(anchorNode)
                            box.renderable = firebaseViewRenderable
                            box.worldPosition = Vector3(modelAnchor.pose.tx(), modelAnchor.pose.compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(), modelAnchor.pose.tz())
                            sceneView.planeRenderer.isVisible = false
                            boxPlaced = true
                        }
                    }
                    textBox.text = getString(R.string.scan_book)
                }
            }

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
        box.renderable = firebaseViewRenderable
        box.localPosition = Vector3(0.0f, 0.25f, 0.0f)
        return base
    }

    //finds the screen center
    private fun Frame.screenCenter(): Vector3{
        val vw = findViewById<View>(android.R.id.content)
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
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

    override fun onPeekTouch(hitTestResult: HitTestResult?, motionEvent: MotionEvent?) {
        if(hitTestResult!!.node == null){
            gestureDetector.onTouchEvent(motionEvent)
        }
    }

    //remove the box node to display the book view, cuz!!
    private fun removeNode(node: Node){
        anchorNode!!.removeChild(node)
    }

    private fun createBookView() {
        if(googleBook == null){
            Toast.makeText(this, "Failed to retrieve book...", Toast.LENGTH_LONG).show()
            return
        }
        removeNode(box)
        boxPlaced = false
        base = Node()
        coinNode = PriceNode(this, googleBook!!)
        coinNode!!.renderable = coinModelRenderable
        coinNode!!.setParent(base)
        coinNode!!.localPosition = Vector3(-0.2f, 0.1f, 0.0f)

        bookNode = BookNode(this, googleBook!!)
        bookNode!!.renderable = bookModelRenderable
        bookNode!!.setParent(base)
        bookNode!!.localPosition = Vector3(0.0f, 0.1f, 0.0f)

        //authorNode = AuthorNode(this, googleBook!!, googleBookItems!!)
        authorNode = Node()
        authorNode!!.setOnTapListener { hitTestResult, motionEvent ->
            addInfoFragment()
        }
        authorNode!!.renderable = infoModelRenderable
        authorNode!!.setParent(base)
        authorNode!!.localPosition = Vector3(0.2f, 0.1f, 0.0f)

        anchorNode!!.addChild(base)
        sceneView.planeRenderer.isVisible = false

    }

    override fun onResume() {
        super.onResume()
        if(sceneView.session == null){
            initializeSession()
        }
        try {
            sceneView.resume()
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
            config.focusMode = Config.FocusMode.AUTO
            session.configure(config)
            sceneView.setupSession(session)
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
            if(bookViewCreated && googleBook != null){
                val favorite = Favorite(
                    googleBook!!.id,
                    userId,
                    googleBook!!.volumeInfo.authors[0],
                    googleBook!!.volumeInfo.title,
                    googleBook!!.volumeInfo.description,
                    googleBook!!.volumeInfo.imageLinks.smallThumbnail
                )
                uiScope.launch {
                        withContext(Dispatchers.IO){
                            repository.addFavoriteAsync(favorite)
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
        sceneView.pause()
        overridePendingTransition(0,0)
        planeDiscoveryController.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        sceneView.destroy()
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

    private fun initiateApiRequests(title:String, author:String, grAuthor:String){
        refJob = uiScope.launch {
            //remove all leading/trailing spaces and "+" in between for correct api args
            googleBook = googleApiGetVolumeAsync("intitle:${title}+inauthor:${author}").items[0]
            goodreadsResponse = goodreadsApiRequestAsync(grAuthor)
        }
    }

    private suspend fun goodreadsApiRequestAsync(arguments: String): GoodreadsResponse=
        withContext(Dispatchers.IO){
            val id:String = repository.goodReadsGetAuthorId(arguments).author.id
            repository.goodReadsAuthorBooksRequest(id)
        }

    private suspend fun googleApiRequestAsync(arguments: String): Items =
        withContext(Dispatchers.IO){
            repository.googleBooksSearchRequest(arguments)
        }

    private suspend fun googleApiGetVolumeAsync(arguments: String): Items =
        withContext(Dispatchers.IO){
            repository.googleBooksGetVolume(arguments)
        }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        Toast.makeText(this, "Initiating flux capacitor...", Toast.LENGTH_LONG).show()
        requestedBookView = true
        if(switch.isChecked && returnBarcode.isNotEmpty()){
            initApiRequestISBN()
        }
        else {
            val title = returnText1.trim().replace("\\s", "+")
            val author = returnText2.trim().replace("\\s", "+")
            //Notify user of api request
            initiateApiRequests(title, author, returnText2.trim())
        }
    }

    private fun initApiRequestISBN(){
        refJob = uiScope.launch {
            googleBook = googleApiGetVolumeAsync("isbn:${returnText1.trim()}").items[0]
            val author = googleBook!!.volumeInfo.authors[0]
            goodreadsResponse = goodreadsApiRequestAsync(author)
        }
    }


    private fun retrieveSignIn(): Long {
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val libraryUserId = sharedPreferences.getLong("userid", 0)

        return when (account == null) {
            true -> libraryUserId
            false -> {
                val gid = account.id.toString()
                val truncate: String = gid.substring(0, gid.length - 12)
                truncate.toLong()
            }
        }
    }

    private fun addInfoFragment(){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val infoFragment = InfoFragment()
        fragmentTransaction.add(R.id.fragment_container, infoFragment)
        fragmentTransaction.setCustomAnimations(R.anim.fragment_fade_enter, R.anim.fragment_fade_exit)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

}
