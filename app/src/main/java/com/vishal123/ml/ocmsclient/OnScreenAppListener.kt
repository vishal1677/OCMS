package com.shubham0204.ml.ocmsclient

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

// on_screen_app goes blank android 10 MIUI 12.0.3


// Checks which app is running in foreground currently.
// It is used with the ForegroundAppService to periodically check which app is visible currently on the
// user's screen
class OnScreenAppListener(private var context: Context ) {

    private var usageStatsManager : UsageStatsManager = context.getSystemService( Context.USAGE_STATS_SERVICE ) as UsageStatsManager
    private var packageManager : PackageManager = context.packageManager
    private var nonSystemAppInfoMap : Map<String,String>

    init {
        nonSystemAppInfoMap = getNonSystemAppsList()
    }

    // https://github.com/ricvalerio/foregroundappchecker/blob/master/fgchecker/src/main/java/com/rvalerio/fgchecker/detectors/LollipopDetector.java

    fun getForegroundApp() : String? {
        var foregroundAppPackageName : String? = null
        val currentTime = System.currentTimeMillis()
        // The `queryEvents` method takes in the `beginTime` and `endTime` to retrieve the usage events.
        // In our case, beginTime = currentTime - 10 minutes ( 1000 * 60 * 10 milliseconds )
        // and endTime = currentTime
        val usageEvents = usageStatsManager.queryEvents( currentTime - (1000*60*10) , currentTime )
        val usageEvent = UsageEvents.Event()
        while ( usageEvents.hasNextEvent() ) {
            usageEvents.getNextEvent( usageEvent )
            if ( usageEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED ) {
                foregroundAppPackageName =
                    if ( nonSystemAppInfoMap.containsKey( usageEvent.packageName ) ) {
                        nonSystemAppInfoMap[ usageEvent.packageName ]
                    }
                    else {
                        null
                    }
            }
        }
        return foregroundAppPackageName
    }

    // We only need non-system apps which were accessed by the user. The Map returned by this method
    // is used to filter the usage history.
    private fun getNonSystemAppsList() : Map<String,String> {
        // TODO : Check getInstalledApplications limitation here
        // https://medium.com/androiddevelopers/package-visibility-in-android-11-cc857f221cd9
        val appInfos = packageManager.getInstalledApplications( PackageManager.GET_META_DATA )
        val appInfoMap = HashMap<String,String>()
        for ( appInfo in appInfos ) {
            // See this SO answer to check if a app is a system app ->
            // https://stackoverflow.com/a/8483920/13546426
            if ( appInfo.flags != ApplicationInfo.FLAG_SYSTEM ) {
                appInfoMap[ appInfo.packageName ]= packageManager.getApplicationLabel( appInfo ).toString()
            }
        }
        return appInfoMap
    }

}