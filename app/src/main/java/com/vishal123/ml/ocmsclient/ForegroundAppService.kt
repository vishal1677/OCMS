package com.shubham0204.ml.ocmsclient

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationChannelCompat

class ForegroundAppService : Service() {

    private val handler = Handler( Looper.getMainLooper() )
    private lateinit var onScreenAppListener : OnScreenAppListener
    private lateinit var firebaseDBManager: FirebaseDBManager
    private val FOREGROUND_APP_CHECK_INTERVAL = 5000L

    override fun onCreate() {
        super.onCreate()
        getSharedPreferences( getString( R.string.app_name ) , Context.MODE_PRIVATE )
            .edit()
            .putBoolean( getString( R.string.service_running_status_key ) , true )
            .apply()
        firebaseDBManager = FirebaseDBManager( this )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun scheduleCheck() {
        handler.postDelayed( runnable , FOREGROUND_APP_CHECK_INTERVAL )
    }

    private val runnable = Runnable() {
        val appName = onScreenAppListener.getForegroundApp()
        if ( !appName.isNullOrBlank() ) {
            firebaseDBManager.updateOnScreenApp( appName ?: "" )
        }
        scheduleCheck()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(100, createNotification() )
        }
        onScreenAppListener = OnScreenAppListener( this )
        scheduleCheck()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val notificationBuilder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    getString(R.string.notification_channel_id),
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(notificationChannel)
                Notification.Builder(this, getString(R.string.notification_channel_id))
            } else {
                Notification.Builder(this)
            }
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let{ notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE )
            }
        return notificationBuilder
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_message))
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks( runnable )
        getSharedPreferences( getString( R.string.app_name ) , Context.MODE_PRIVATE )
            .edit()
            .putBoolean( getString( R.string.service_running_status_key ) , true )
            .apply()
    }




}