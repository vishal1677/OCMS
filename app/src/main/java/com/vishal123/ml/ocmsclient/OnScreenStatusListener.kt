package com.shubham0204.ml.ocmsclient

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import kotlin.math.roundToInt

class OnScreenStatusListener(
    private var activityLifecycle : Lifecycle ,
    callback : Callback ) : LifecycleObserver {

    private var timeWhenBackground = 0L
    private var userInBackgroundThreshold = 5000L

    interface Callback {
        fun inForeground( secondsSinceBackground : Int? )
        fun inBackground()
    }

    fun removeObserver() {
        activityLifecycle.removeObserver( lifecycleEventObserver )
    }

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME ) {
            val millisSinceBackground = System.currentTimeMillis() - timeWhenBackground
            if ( millisSinceBackground < userInBackgroundThreshold ) {
                callback.inForeground( null )
            }
            else {
                callback.inForeground( (millisSinceBackground / 1000f).roundToInt() )
            }
        }
        else if ( event == Lifecycle.Event.ON_PAUSE ) {
            callback.inBackground()
            timeWhenBackground = System.currentTimeMillis()
        }
    }

    init {
        activityLifecycle.addObserver( lifecycleEventObserver )
    }




}