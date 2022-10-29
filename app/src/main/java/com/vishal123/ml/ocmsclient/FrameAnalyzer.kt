package com.shubham0204.ml.ocmsclient

import android.graphics.*
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class FrameAnalyzer( private val firebaseDBManager: FirebaseDBManager ) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode( FaceDetectorOptions.PERFORMANCE_MODE_FAST )
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)
    private lateinit var currentFrameImage : ImageProxy
    private var isProcessing = false

    private var prevPresenceStatus = "Present"
    private var prevPresenceStatusCount = 0
    private val lazyUpdateThreshold = 5

    override fun analyze(image: ImageProxy) {
        Log.e( "APP" , "started" )
        currentFrameImage = image
        if ( !isProcessing ) {
            isProcessing = true
            val inputImage = InputImage.fromMediaImage( image.image!! , image.imageInfo.rotationDegrees )
            detector.process( inputImage )
                .addOnSuccessListener { faces ->
                    if ( faces.size == 1 ) {
                        lazyUpdatePresenceStatus( "Present" )
                        Log.e( "APP" , "present" )
                    }
                    else {
                        lazyUpdatePresenceStatus( "Absent" )
                        Log.e( "APP" , "absent" )
                    }
                }
                .addOnFailureListener {  exception ->

                }
                .addOnCompleteListener {
                    isProcessing = false
                    currentFrameImage.close()
                }
        }
        else {
            currentFrameImage.close()
        }
    }

    private fun lazyUpdatePresenceStatus( status : String ) {
        Log.e( "APP" , "status $status")
        if ( status == prevPresenceStatus ) {
            prevPresenceStatusCount += 1
        }
        else {
            prevPresenceStatusCount = 0
        }
        prevPresenceStatus = status
        if ( prevPresenceStatusCount > lazyUpdateThreshold ) {
            firebaseDBManager.updatePresenceStatus( status )
            prevPresenceStatusCount = 0
        }
    }

}