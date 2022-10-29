package com.shubham0204.ml.ocmsclient

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.shubham0204.ml.ocmsclient.databinding.ActivityMainBinding
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding : ActivityMainBinding
    private lateinit var onScreenAppListener : OnScreenAppListener
    private lateinit var onScreenStatusListener: OnScreenStatusListener
    private lateinit var frameAnalyzer: FrameAnalyzer
    private lateinit var firebaseDBManager: FirebaseDBManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationManager : NotificationManager
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var previewUseCase : UseCase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate( layoutInflater )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView( viewBinding.root )

        onScreenAppListener = OnScreenAppListener( this )
        firebaseDBManager = FirebaseDBManager( this )
        frameAnalyzer = FrameAnalyzer( firebaseDBManager )
        sharedPreferences = getSharedPreferences( getString( R.string.app_name ) , Context.MODE_PRIVATE )
        notificationManager = getSystemService( Context.NOTIFICATION_SERVICE ) as NotificationManager
        onScreenStatusListener = OnScreenStatusListener( lifecycle , activityLifecycleCallback )

        startCameraPreview()

        if ( checkUsageStatsPermission() ) {
            // Implement further app logic here ...
        }
        else {
            val alertDialog = MaterialAlertDialogBuilder( this ).apply {
                setTitle( getString(R.string.usage_stats_display))
                setMessage( getString(R.string.usage_stats_des_display) )
                setCancelable( false )
                setPositiveButton( getString( R.string.allow_display )) { dialog, which ->
                    dialog.dismiss()
                    Intent( Settings.ACTION_USAGE_ACCESS_SETTINGS ).apply {
                        startActivity( this )
                    }
                }
                setNegativeButton( getString( R.string.close_display) ) { dialog, which ->
                    dialog.dismiss()
                    finish()
                }
                create()
            }
            alertDialog.show()
        }

        val result = FirebaseAuth.getInstance().signInAnonymously()
        if ( result.isSuccessful ) {
            Log.e( "APP" , "success" )
        }

        /*
        if ( checkNotificationAccessPermission() ) {
            notificationManager.setInterruptionFilter( NotificationManager.INTERRUPTION_FILTER_NONE )
        }
        else {
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                startActivity( this )
            }
        }*/
        viewBinding.cameraPreviewview.isClickable = false
        viewBinding.leaveMeetingButton.setOnClickListener { leaveMeeting() }

    }


    private val activityLifecycleCallback = object : OnScreenStatusListener.Callback {

        override fun inForeground(secondsSinceBackground: Int?) {
            firebaseDBManager.updateOnScreenStatus( true )
            notifyPermissionsStatus()
            if ( sharedPreferences.getBoolean( getString( R.string.service_running_status_key ) , false ) ) {
                stopService( Intent( this@MainActivity , ForegroundAppService::class.java) )
            }
        }

        override fun inBackground() {
            firebaseDBManager.updateOnScreenStatus( false )
            val foregroundAppServiceIntent = Intent( this@MainActivity , ForegroundAppService::class.java)
            if ( checkUsageStatsPermission() ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(foregroundAppServiceIntent)
                }
                else {
                    startService( foregroundAppServiceIntent )
                }
            }
        }

    }

    override fun onBackPressed() {
        val alertDialog = MaterialAlertDialogBuilder( this ).apply {
            setTitle( "Leave Meeting")
            setMessage( "Are you sure you want to leave the meeting?" )
            setCancelable( false )
            setPositiveButton( "LEAVE" ) { dialog, which ->
                dialog.dismiss()
                leaveMeeting()
            }
            setNegativeButton( "CLOSE" ) { dialog, which ->
                dialog.dismiss()
            }
            create()
        }
        alertDialog.show()
    }

    private fun leaveMeeting() {
        onScreenStatusListener.removeObserver()
        cameraProvider.unbindAll()
        firebaseDBManager.updateMeetingStatus()
        if ( sharedPreferences.getBoolean( getString( R.string.service_running_status_key ) , false ) ) {
            stopService( Intent( this@MainActivity , ForegroundAppService::class.java) )
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveMeeting()
    }

    // Notify FirebaseDBManager regarding any changes in camera, audio or usage stats permissions.
    // This method is called everytime the visibility of the app changes.
    private fun notifyPermissionsStatus() = firebaseDBManager.apply {
        updateCameraPermissionStatus( checkCameraPermission() )
        updateAudioPermissionStatus( checkAudioPermission() )
        updateAppUsagePermissionStatus( checkUsageStatsPermission() )
        updateNotificationAccessPermissionStatus( checkNotificationAccessPermission() )
    }

    // The `PACKAGE_USAGE_STATS` permission is a not a runtime permission and hence cannot be
    // requested directly using `ActivityCompat.requestPermissions`. All special permissions
    // are handled by `AppOpsManager`.
    private fun checkUsageStatsPermission() : Boolean {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        // `AppOpsManager.checkOpNoThrow` is deprecated from Android Q
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                "android:get_usage_stats",
                Process.myUid(), packageName
            )
        }
        else {
            appOpsManager.checkOpNoThrow(
                "android:get_usage_stats",
                Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // Check if the permission for Do Not Disturb is enabled
    // See this SO answer -> https://stackoverflow.com/a/36162332/13546426
    private fun checkNotificationAccessPermission() : Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    // Check if the camera permission has been granted by the user.
    private fun checkCameraPermission() : Boolean = checkSelfPermission( Manifest.permission.CAMERA ) ==
            PackageManager.PERMISSION_GRANTED

    // Check if the audio permission has been granted by the user.
    private fun checkAudioPermission() : Boolean = checkSelfPermission( Manifest.permission.RECORD_AUDIO ) ==
            PackageManager.PERMISSION_GRANTED

    private fun startCameraPreview() {
        Log.e( "APP" , "Camera started" )
        val cameraProviderFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview : Preview = Preview.Builder().build()
            val cameraSelector : CameraSelector = CameraSelector.Builder()
                .requireLensFacing( CameraSelector.LENS_FACING_FRONT )
                .build()
            preview.setSurfaceProvider( viewBinding.cameraPreviewview.surfaceProvider )
            val imageFrameAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio( AspectRatio.RATIO_4_3 )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyzer )
            previewUseCase = preview
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview , imageFrameAnalysis )
        }, ContextCompat.getMainExecutor(this) )
    }

}

