package com.example.library.home

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*


object Utils {

    fun displayError(context: Context, errorMsg: String, problem: Throwable?){
        val tag = context.applicationInfo.className
        val toastText : String
        if(problem != null && problem.message != null){
            Log.e(tag, errorMsg, problem)
            toastText = errorMsg + ": " + problem.message
        } else if (problem != null){
            Log.e(tag, errorMsg, problem)
        } else {
            Log.e(tag, errorMsg)
            toastText = errorMsg
        }

        Handler(Looper.getMainLooper()).post { ->
            var toast = Toast.makeText(context, errorMsg, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    fun createArSession(activity: Activity, installRequested: Boolean): Session?{
        var session : Session?
        session = null
        if(hasCameraPermission(activity)){
            var case = ArCoreApk.getInstance().requestInstall(activity, !installRequested)
            if(case == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                return null
            }
            session = Session(activity)
            var config = Config(session)
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE)
            session.configure(config)
        }
        return session
    }

    fun requestCameraPermission(activity: Activity, requestCode: Int){
        var manifestPer = Manifest.permission.CAMERA
        ActivityCompat.requestPermissions(activity, arrayOf(manifestPer), requestCode)
    }

    fun hasCameraPermission(activity: Activity): Boolean{
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldSHowREquestPermissionRational(activity: Activity): Boolean{
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    fun launchPermissionSettings(activity: Activity){
        var intent = Intent()
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.fromParts("package", activity.packageName, null))
        activity.startActivity(intent)
    }

    fun handleSessionException(activity: Activity, sessionException: UnavailableException){
        var message: String
        if(sessionException is UnavailableApkTooOldException){
            message = "Please install ARCore"
        } else if(sessionException is UnavailableSdkTooOldException){
            message = "Please update this app"
        } else if(sessionException is UnavailableArcoreNotInstalledException){
            message = "Please install ARCore"
        } else if(sessionException is UnavailableDeviceNotCompatibleException){
            message = "This device does not support AR"
        } else {
            message = "Failed to create AR session"
            Log.e("TAG", "Exception : " + sessionException)
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean{
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
            Log.e("TAG", "Sceneform requires Andorid N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }

        var activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var openGlVersionString = activityManager.deviceConfigurationInfo.glEsVersion
        var doubleGl = openGlVersionString.toDoubleOrNull()
        if(doubleGl != null && doubleGl < 3.0){
            Log.e("TAG", "Sceneform requires OpenGl ES 3.0 or later")
            Toast.makeText(activity, "Sceneform requires OpenGl ES 3.0 or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        return true
    }

}