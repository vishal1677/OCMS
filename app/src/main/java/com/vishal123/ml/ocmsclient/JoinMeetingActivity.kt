package com.shubham0204.ml.ocmsclient

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import com.shubham0204.ml.ocmsclient.databinding.ActivityJoinMeetingBinding
import java.util.*

class JoinMeetingActivity : AppCompatActivity() {

    private lateinit var joinMeetingBinding : ActivityJoinMeetingBinding
    private lateinit var notificationManager : NotificationManager
    private lateinit var userID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        joinMeetingBinding = ActivityJoinMeetingBinding.inflate( layoutInflater )
        setContentView( joinMeetingBinding.root )

        userID = createUser()

        notificationManager = getSystemService( Context.NOTIFICATION_SERVICE) as NotificationManager

        joinMeetingBinding.joinMeetingButton.apply {
            setOnClickListener( onJoinMeetingClickListener )
            isEnabled = false
        }

        if ( !checkCameraPermission() ) {
            requestCameraPermission()
        }

        setTextListenerOnJoinButton()



    }

    private fun createUser() : String {
        val userID = UUID.randomUUID().toString()
        getSharedPreferences( getString( R.string.app_name ) , Context.MODE_PRIVATE )
            .edit()
            .putString( "user_id" , userID )
            .apply()
        return userID
    }

    private val onJoinMeetingClickListener = View.OnClickListener {
        Intent( this , MainActivity::class.java ).apply {
            val userName = joinMeetingBinding.nameTextInputEdittext.run{
                this.clearFocus()
                this.text.toString()
            }
            setUserNameInFirebaseDB( userID , userName )
            this.putExtra( "user_name" , userName )
            startActivity( this )
        }
    }

    private fun setUserNameInFirebaseDB( userID : String , userName : String ) {
        FirebaseDatabase.getInstance()
            .reference
            .child( userID ).apply {
                child( "name" ).setValue( userName )
                child( "is_active" ).setValue( true )
                child( "start" ).setValue( System.currentTimeMillis() )
            }
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

    private fun requestCameraPermission() {
        cameraPermissionRequestLauncher.launch( Manifest.permission.CAMERA )
    }

    private val cameraPermissionRequestLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission() ) {
            isGranted ->
        if ( isGranted ) {

        }
        else {
            val alertDialog = MaterialAlertDialogBuilder( this ).apply {
                setTitle( getString(R.string.camera_permission_display))
                setMessage( getString(R.string.camera_permission_des_display) )
                setCancelable( false )
                setPositiveButton( getString(R.string.allow_display) ) { dialog, which ->
                    dialog.dismiss()
                    requestCameraPermission()
                }
                setNegativeButton( getString(R.string.close_display) ) { dialog, which ->
                    dialog.dismiss()
                    finish()
                }
                create()
            }
            alertDialog.show()
        }
    }

    private fun setTextListenerOnJoinButton() {
        joinMeetingBinding.nameTextInputEdittext.addTextChangedListener( object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                joinMeetingBinding.joinMeetingButton.isEnabled = s!!.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
    }

}