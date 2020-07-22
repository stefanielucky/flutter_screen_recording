package com.isvisoft.flutter_screen_recording

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.IOException


class FlutterScreenRecordingPlugin(
        private val registrar: Registrar
) : MethodCallHandler,
        PluginRegistry.ActivityResultListener {

    var mScreenDensity: Int = 0
    var mMediaRecorder: MediaRecorder? = null
    var mProjectionManager: MediaProjectionManager? = null
    var mMediaProjection: MediaProjection? = null
    var mMediaProjectionCallback: MediaProjectionCallback? = null
    var mVirtualDisplay: VirtualDisplay? = null
    var mDisplayWidth: Int = 1280
    var mDisplayHeight: Int = 800
    var storePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + File.separator
    var videoName: String? = ""
    var recordAudio: Boolean? = false
    var width : Int? = 1280
    var height : Int? = 800
    var bitRate: Int? = 5
    var frameRate: Int? = 60;
    private val SCREEN_RECORD_REQUEST_CODE = 333
    private val SCREEN_STOP_RECORD_REQUEST_CODE = 334

    private lateinit var _result: MethodChannel.Result


    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_screen_recording")
            val plugin = FlutterScreenRecordingPlugin(registrar)
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mMediaProjectionCallback = MediaProjectionCallback()
                mMediaProjection = mProjectionManager?.getMediaProjection(resultCode, data)
                mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
                mVirtualDisplay = createVirtualDisplay()
                _result.success(true)
                return true
            } else {
                _result.success(false)
            }
        }

        return false
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "startRecordScreen") {
            try {
                _result = result
                mMediaRecorder = MediaRecorder()
                mProjectionManager = registrar.context().applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?
                val windowManager = registrar.context().applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics: DisplayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                mScreenDensity = metrics.densityDpi

                calculeResolution(metrics)

                videoName = call.argument<String?>("name")
                recordAudio = call.argument<Boolean?>("audio")
                width = call.argument<Int?>("width")
                height = call.argument<Int?>("height")
                bitRate = call.argument<Int?>("bitRate")
                frameRate = call.argument<Int?>("frameRate")
               
                startRecordScreen()
                //result.success(true)
            } catch (e: Exception) {
                println("Error onMethodCall startRecordScreen")
                println(e.message)
                result.success(false)
            }

        } else if (call.method == "stopRecordScreen") {
            try {
                if (mMediaRecorder != null) {
                    stopRecordScreen()
                    result.success("${storePath}${videoName}.mp4")
                } else {
                    result.success("")
                }
            } catch (e: Exception) {
                result.success("")
            }

        } else {
            result.notImplemented()
        }
    }

    fun calculeResolution(metrics: DisplayMetrics) {

        var maxRes = 1280.0;

        if (metrics.scaledDensity >= 3.0f) {
            maxRes = 1920.0;
        }

        if (metrics.widthPixels > metrics.heightPixels) {
            val rate = metrics.widthPixels / maxRes
            mDisplayWidth = maxRes.toInt()
            mDisplayHeight = (metrics.heightPixels / rate).toInt()
        } else {
            val rate = metrics.heightPixels / maxRes
            mDisplayHeight = maxRes.toInt()
            mDisplayWidth = (metrics.widthPixels / rate).toInt()
        }

        println("Scaled Density")
        println(metrics.scaledDensity)
        println("Original Resolution ")
        println(metrics.widthPixels.toString() + " x " + metrics.heightPixels)
        println("Calcule Resolution ")
        println("$mDisplayWidth x $mDisplayHeight")
       
    }

    fun startRecordScreen() {
        try {
            println("$width.....$height.....$bitRate.....$frameRate ${width!=null}")
            println("${width is Int}.....${height is Int}.....${bitRate is Int}.....${frameRate is Int} ${width!=null}")
            mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            if (recordAudio!!) {
                mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            } else {
                mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            }
            mMediaRecorder?.setOutputFile("${storePath}${videoName}.mp4")
            var w: Int = 650
            var h: Int = 1280
            var b: Int = 5
            var f: Int = 60
            if(width!=null){
                w=width as Int
            }
            if(height!=null){
                h=height as Int
            }
            if(bitRate!=null){
                b=bitRate as Int
            }
            if(frameRate!=null){
                f=frameRate as Int
            }

            // val w :Int=width as Int
            // val h :Int=height as Int
            // val b :Int=bitRate as Int
            // val f :Int=frameRate as Int
            mMediaRecorder?.setVideoSize(w, h)
            mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder?.setVideoEncodingBitRate(b * w * h)
            mMediaRecorder?.setVideoFrameRate(f)
            // if(w==null&&h==null){
            //     mMediaRecorder?.setVideoSize(mDisplayWidth, mDisplayHeight)
            // }else if(h==null){
            //     mMediaRecorder?.setVideoSize(w, mDisplayHeight)
            // }else if(w==null){
            //     mMediaRecorder?.setVideoSize(mDisplayWidth, h)
            // }else{
            //     mMediaRecorder?.setVideoSize(w, h)
            // }
            
            // mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            // if(b==null){
            //     mMediaRecorder?.setVideoEncodingBitRate(5 * w * h)
            // }else{
            //     mMediaRecorder?.setVideoEncodingBitRate(b * mDisplayWidth * mDisplayHeight)
            // }

            
           
            if(f!=null){
                mMediaRecorder?.setVideoFrameRate(f)
            }else{
                
                mMediaRecorder?.setVideoFrameRate(60)
            }
            


            mMediaRecorder?.prepare()
            mMediaRecorder?.start()
        } catch (e: IOException) {
            Log.d("--INIT-RECORDER", e.message)
            println("Error startRecordScreen")
            println(e.message)
        }

        val permissionIntent = mProjectionManager?.createScreenCaptureIntent()
//        ActivityCompat.startActivityForResult((registrar.context().applicationContext as FlutterApplication).currentActivity, permissionIntent!!, SCREEN_RECORD_REQUEST_CODE, null)
        ActivityCompat.startActivityForResult(registrar.activity(), permissionIntent!!, SCREEN_RECORD_REQUEST_CODE, null)

    }

    fun stopRecordScreen() {
        try {

            mMediaRecorder?.stop()
            mMediaRecorder?.reset()
            println("stopRecordScreen success")

        } catch (e: Exception) {
            Log.d("--INIT-RECORDER", e.message)
            println("stopRecordScreen error")
            println(e.message)

        } finally {
            stopScreenSharing()
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        var w: Int = 650
        var h: Int = 1280
        if(width!=null){
            w=width as Int
        }
        if(height!=null){
            h=height as Int
        }
        return mMediaProjection?.createVirtualDisplay("MainActivity", w, h, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder?.getSurface(), null, null)
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay?.release()
            if (mMediaProjection != null) {
                mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
                mMediaProjection?.stop()
                mMediaProjection = null
            }
            Log.d("TAG", "MediaProjection Stopped")
        }
    }

    inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            mMediaRecorder?.stop()
            mMediaRecorder?.reset()

            mMediaProjection = null
            stopScreenSharing()
        }
    }

}