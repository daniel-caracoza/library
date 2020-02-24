package com.example.library

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.*
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.opengles.GL10
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig

class ArOverlayView : Fragment(), SurfaceTexture.OnFrameAvailableListener, ImageReader.OnImageAvailableListener, GLSurfaceView.Renderer{
    private lateinit var arSession : Session
    private lateinit var planeRenderer: PlaneRenderer
    private lateinit var glSurface : GLSurfaceView
    private lateinit var tapHelper : TapHelper
    private lateinit var shader : ShaderUtil
    private lateinit var camManager : CameraManager
    private lateinit var camDevice : CameraDevice
    private lateinit var sharedCam : SharedCamera
    private lateinit var backgroundThread : HandlerThread
    private lateinit var backgroundHandler : Handler
    private lateinit var cpuImageReader : ImageReader
    private lateinit var cameraId : String
    private lateinit var captureSession : CameraCaptureSession
    private lateinit var displayRotationHelper : DisplayRotationHelper
    private lateinit var previewCaptureRequestBuilder : CaptureRequest.Builder
    private lateinit var trackingStateHelper : TrackingStateHelper
    private var backgroundRenderer = BackgroundRenderer()
    private var pointCloudRenderer = PointCloudRenderer()
    private val anchorMatrix = FloatArray(16)
    private var cpuImagesProcessed = 0
    private var arcoreActive = true
    private var isGlAttached = false
    private var surfaceCreated = false

    private val anchors = ArrayList<ColoredAnchor>()
    private var captureSessionChangesPossible = true
    private val virtualObject = ObjectRenderer()
    private val shouldUpdateSurfaceTexture = AtomicBoolean(false)

    private class ColoredAnchor(val anchor : Anchor, val color : FloatArray)

    private var cameraCaptureCallback = object: CameraCaptureSession.CaptureCallback(){
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            shouldUpdateSurfaceTexture.set(true)
        }

        override fun onCaptureBufferLost(
            session: CameraCaptureSession,
            request: CaptureRequest,
            target: Surface,
            frameNumber: Long
        ) {
            Log.e("Capture Callback: ", "onCaptureBufferLost: " + frameNumber)
        }

        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
            Log.e("Capture Callback: ", "onCaptureSequenceAborted: " + sequenceId)
        }
    }

    var cameraSessionStateCallback = object: CameraCaptureSession.StateCallback(){
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            setRepeatingCaptureRequest()
        }

        override fun onReady(session: CameraCaptureSession) {
            super.onReady(session)
        }

        override fun onActive(session: CameraCaptureSession) {
            if(!arcoreActive){
                resumeARCore()
            }
            synchronized(this@ArOverlayView){
                captureSessionChangesPossible = true
            }
        }

        override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
            Log.w("StateCallback: ", "Camera capture queue empty")
        }

        override fun onClosed(session: CameraCaptureSession) {
            Log.d("StateCallback: ", "Camera capture session closed")
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e("StateCallback: ", "Failed to configure camera capture session")
        }
    }

    private var cameraDeviceCallback = object: CameraDevice.StateCallback(){
        override fun onOpened(cameraDevice: CameraDevice){
            this@ArOverlayView.camDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraDevice.close()
        }
    }

    override fun onStart() {
        super.onStart()
        waitUntilCameraCaptureSessionIsActive()
        //startBackgroundThread()

        displayRotationHelper.onResume()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        //surfaceTexture?.updateTexImage()
    }

    override fun onImageAvailable(imageReader: ImageReader?) {
        val image = imageReader?.acquireLatestImage()
        if(image == null){
            return
        }
        image.close()
        cpuImagesProcessed++
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0, width, height)
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        surfaceCreated = true

        GLES20.glClearColor(0f, 0f, 0f, 1.0f)

        try{
            backgroundRenderer.createOnGlThread(this@ArOverlayView.requireContext())
            planeRenderer.createOnGlThread(this@ArOverlayView.requireContext(), "sampledata/models/trigrid.png")
            pointCloudRenderer.createOnGlThread(this@ArOverlayView.requireContext())

            virtualObject.createOnGlThread(this@ArOverlayView.requireContext(), "sampledata/models/andy.obj", "sampledata/models/andy.png")
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f)

            //openCamera()
        } catch (e : IOException){
            Log.e("Surface Created: ", "Failed to read an asset file")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.aroverlay_layout, container, false)
        glSurface = view.findViewById(R.id.glsurfaceview)
        glSurface.preserveEGLContextOnPause = true
        glSurface.setEGLContextClientVersion(2)
        glSurface.setEGLConfigChooser(8,8,8,8,16,0)

        glSurface.setRenderer(this@ArOverlayView)
        glSurface.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        displayRotationHelper = DisplayRotationHelper(this@ArOverlayView.requireContext())
        tapHelper = TapHelper(this@ArOverlayView.requireContext())
        glSurface.setOnTouchListener(tapHelper)

        trackingStateHelper = TrackingStateHelper(activity as Activity)

        arSession = Session(view.context, EnumSet.of(Session.Feature.SHARED_CAMERA))
        openCamera()
        openCameraForSharing()
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shader = ShaderUtil
        startBackgroundThread()

        Log.d("Fragment", "OnCreate")
    }

    override fun onActivityCreated(mState: Bundle?) {
        super.onActivityCreated(mState)
        //glSurface.setRenderer(this@ArOverlayView)
    }

    private fun setRepeatingCaptureRequest(){
        try{
            captureSession.setRepeatingRequest(
                previewCaptureRequestBuilder.build(),
                cameraCaptureCallback,
                backgroundHandler
            )
        } catch (e : CameraAccessException){
            Log.e("RepreatingCaptureRequest ", "Failed to set repeating request")
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if(!shouldUpdateSurfaceTexture.get()){
            return
        }

        displayRotationHelper.updateSessionIfNeeded(arSession)

        try{
            onDrawFrameARCore()

        } catch (e : Throwable){
            Log.e("OPenGLthread", "Exception on the OpenGL thread")
        }
    }

    private fun onDrawFrameARCore(){
        if(!arcoreActive){
            //return
        }

        val frame = arSession.update()
        val camera = frame.camera

        isGlAttached = true

        handleTap(frame, camera)

        backgroundRenderer.draw(frame)

        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        if(camera.trackingState == TrackingState.PAUSED){
            return
        }

        val projmtx = FloatArray(16)
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

        val viewmtx = FloatArray(16)
        camera.getViewMatrix(viewmtx, 0)

        val colorCorrectionRgba = FloatArray(4)
        frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)

        val pointCloud = frame.acquirePointCloud()
        pointCloudRenderer.update(pointCloud)
        pointCloudRenderer.draw(viewmtx, projmtx)

        planeRenderer.drawPlanes(arSession.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projmtx)

        val scaleFactor = 1.0f
        for(coloredAnchor in anchors){
            if(coloredAnchor.anchor.trackingState != TrackingState.TRACKING){
                continue
            }
            coloredAnchor.anchor.pose.toMatrix(anchorMatrix, 0)

            virtualObject.updateModelMatrix(anchorMatrix, scaleFactor)
            virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color)
        }
    }

    private fun handleTap(frame : Frame, camera : Camera){
        val tap = tapHelper.poll()
        if(tap != null && camera.trackingState == TrackingState.TRACKING){
            for(hit in frame.hitTest(tap)){
                val trackable = hit.trackable
                if(trackable is Plane
                    && trackable.isPoseInPolygon(hit.hitPose)
                    && PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0
                    || trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL){
                    if(anchors.size >= 20){
                        anchors.get(0).anchor.detach()
                        anchors.removeAt(0)
                    }
                    val objColor = FloatArray(4)
                    if(trackable is Point){
                        objColor.set(0, 66.0f)
                        objColor.set(1, 133.0f)
                        objColor.set(2, 244.0f)
                        objColor.set(3, 255.0f)
                    } else if (trackable is Plane){
                        objColor.set(0, 139.0f)
                        objColor[1] = 195.0f
                        objColor.set(2, 74.0f)
                        objColor.set(3, 255.0f)
                    } else {
                        objColor.set(0, 0f)
                        objColor.set(1, 0f)
                        objColor.set(2, 0f)
                        objColor.set(3, 0f)
                    }
                    val a = ColoredAnchor(hit.createAnchor(), objColor)
                    anchors.add(a)
                    break
                }
            }
        }
    }

    private fun openCameraForSharing(){
        sharedCam = arSession.sharedCamera
        if(context?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity as Activity, arrayOf(Manifest.permission.CAMERA), 0)
        }
        camManager.openCamera(
            arSession.cameraConfig.cameraId,
            sharedCam.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler),
            backgroundHandler
        )
    }

    private fun waitUntilCameraCaptureSessionIsActive(){
        while(!captureSessionChangesPossible){
            try{
                SystemClock.sleep(100)
            } catch (e : InterruptedException){
                Log.e("Wait: ", "Unable to wait for a safe time to make changes")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        waitUntilCameraCaptureSessionIsActive()
        startBackgroundThread()
        //glsurfaceview.onResume()

        if(surfaceCreated){
            //openCamera()
        }

        displayRotationHelper.onResume()
    }

    private fun openCamera(){
            try{
                arSession = Session(context, EnumSet.of(Session.Feature.SHARED_CAMERA))
            } catch (e : KeyCharacterMap.UnavailableException){
                return
            }
            sharedCam = arSession.sharedCamera
            cameraId = arSession.cameraConfig.cameraId
            val desiredCpuImageSize = arSession.cameraConfig.imageSize
            cpuImageReader = ImageReader.newInstance(
                desiredCpuImageSize.width,
                desiredCpuImageSize.height,
                ImageFormat.YUV_420_888,
                2
            )
            cpuImageReader.setOnImageAvailableListener(this, backgroundHandler)
            sharedCam.setAppSurfaces(this.cameraId, Arrays.asList(cpuImageReader.surface))

            try{
                val wrappedCallback = sharedCam.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler)
                camManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

                captureSessionChangesPossible = false

                if(context?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(activity as Activity, arrayOf(Manifest.permission.CAMERA), 0)
                }
                camManager.openCamera(cameraId, wrappedCallback, backgroundHandler)
            } catch (e : CameraAccessException) {
                Log.e("CameraOpen: ", "Failed to open camera")
            }
    }

    private fun startBackgroundThread(){
        backgroundThread = HandlerThread("sharedCameraBackground")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun createCameraPreviewSession(){
        try{
            arSession.setCameraTextureName(backgroundRenderer.textureId)
            sharedCam.surfaceTexture.setOnFrameAvailableListener(this)
            previewCaptureRequestBuilder = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val surfaceList = sharedCam.arCoreSurfaces
            surfaceList.add(cpuImageReader.surface)
            for(surface in surfaceList){
                previewCaptureRequestBuilder.addTarget(surface)
            }
            val wrappedCallback = sharedCam.createARSessionStateCallback(cameraSessionStateCallback, backgroundHandler)
            camDevice.createCaptureSession(surfaceList, wrappedCallback, backgroundHandler)
        } catch (e : CameraAccessException){
            Log.e("Create Camera Preview Session: ", e.toString())
        }
    }

    private fun resumeARCore(){
        if(!arcoreActive){
            try{
                backgroundRenderer.suppressTimestampZeroRendering(false)
                arSession.resume()
                arcoreActive = true
                sharedCam.setCaptureCallback(cameraCaptureCallback, backgroundHandler)
            } catch (e : CameraNotAvailableException){
                Log.e("Resume ARCore: ", "Failed to resume ARCore session", e)
                return
            }
        }
    }
}