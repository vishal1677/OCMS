package com.shubham0204.ml.ocmsclient

import android.content.Context
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class FirebaseDBManager( private var context: Context ) {

    private var userDBReference: DatabaseReference
    private val ON_SCREEN_STATUS = "on_screen_status"
    private val ON_SCREEN_APP = "on_screen_app"
    private val CAMERA_PERMISSION_STATUS = "camera_status"
    private val AUDIO_PERMISSION_STATUS = "audio_status"
    private val USAGE_STATS_PERMISSION_STATUS = "usage_status"
    private val LOCATION_STATUS = "location"
    private val PRESENCE_STATUS = "presence"
    private val NOTIFICATION_ACCESS_PERMISSION_STATUS = "notification_access"
    private val IS_ACTIVE = "is_active"

    init {
        userDBReference = FirebaseDatabase.getInstance().reference.child(getUserID())
    }

    private fun getUserID(): String {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_id", "")!!
    }

    fun updateMeetingStatus() = updateStudentStat( IS_ACTIVE , false )

    fun updateOnScreenStatus(status: Boolean) = updateStudentStat(ON_SCREEN_STATUS, status)

    fun updateOnScreenApp(appName: String) = updateStudentStat(ON_SCREEN_APP, appName)

    fun updateNotificationAccessPermissionStatus(isEnabled: Boolean) = updateStudentStat(NOTIFICATION_ACCESS_PERMISSION_STATUS, isEnabled)

    fun updateCameraPermissionStatus(isEnabled: Boolean) = updateStudentStat(CAMERA_PERMISSION_STATUS, isEnabled)

    fun updateAudioPermissionStatus(isEnabled: Boolean) = updateStudentStat(AUDIO_PERMISSION_STATUS, isEnabled)

    fun updateAppUsagePermissionStatus(isEnabled: Boolean) = updateStudentStat(USAGE_STATS_PERMISSION_STATUS, isEnabled)

    fun updatePresenceStatus(status: String) = updateStudentStat(PRESENCE_STATUS, status)

    fun updateLocationStatus(localityName: String) = updateStudentStat(LOCATION_STATUS, localityName)

    private fun updateStudentStat(statKey: String, value: Any) {
        CoroutineScope(Dispatchers.Default).launch{
            userDBReference.child(statKey)
                .setValue(value)
                .addOnSuccessListener {
                    Log.e("App", "updated")
                }
                .addOnFailureListener { exception ->
                    Log.e("App", "exception ${exception.message}")
                }
        }
    }

}