package com.adityaarora.liveedgedetection.view

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.shapes.PathShape
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PictureCallback
import android.hardware.Camera.PreviewCallback
import android.hardware.Camera.ShutterCallback
import android.media.AudioManager
import android.os.CountDownTimer
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import com.adityaarora.liveedgedetection.constants.ScanConstants
import com.adityaarora.liveedgedetection.enums.ScanHint
import com.adityaarora.liveedgedetection.interfaces.IScanner
import com.adityaarora.liveedgedetection.util.ImageDetectionProperties
import com.adityaarora.liveedgedetection.util.ScanUtils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.IOException
import kotlin.math.roundToInt

/**
 * This class previews the live images from the camera
 */
class ScanSurfaceView(context: Context, iScanner: IScanner) : FrameLayout(context), SurfaceHolder.Callback {

    var mSurfaceView: SurfaceView
    private val scanCanvasView: ScanCanvasView
    private var vWidth = 0
    private var vHeight = 0
    private var camera: Camera? = null
    private val iScanner: IScanner
    private var autoCaptureTimer: CountDownTimer? = null
    private var secondsLeft = 0
    private var isAutoCaptureScheduled = false
    private var previewSize: Camera.Size? = null
    private var isCapturing = false

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            requestLayout()
            openCamera()
            camera?.setPreviewDisplay(holder)
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        }
    }

    fun clearAndInvalidateCanvas() {
        scanCanvasView.clear()
        invalidateCanvas()
    }

    fun invalidateCanvas() {
        scanCanvasView.invalidate()
    }

    private fun openCamera() {
        if (camera == null) {
            val info = CameraInfo()
            var defaultCameraId = 0
            for (i in 0 until Camera.getNumberOfCameras()) {
                Camera.getCameraInfo(i, info)
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    defaultCameraId = i
                }
            }
            camera = Camera.open(defaultCameraId)
            val cameraParams = camera!!.parameters
            val flashModes = cameraParams.supportedFlashModes
            if (null != flashModes && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                cameraParams.flashMode = Camera.Parameters.FLASH_MODE_AUTO
            }
            camera!!.parameters = cameraParams
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (vWidth == vHeight) {
            return
        }
        if (previewSize == null) previewSize = ScanUtils.getOptimalPreviewSize(camera, vWidth, vHeight)
        val parameters = camera!!.parameters

        camera?.let { cam ->
            (context as? Activity)?.let { act ->
                cam.setDisplayOrientation(ScanUtils.configureCameraAngle(act))
            }
            parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)
            if (parameters.supportedFocusModes != null
                    && parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            } else if (parameters.supportedFocusModes != null
                    && parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
            val size = ScanUtils.determinePictureSize(cam, parameters.previewSize)
            size?.let {
                parameters.setPictureSize(size.width, size.height)
                parameters.pictureFormat = ImageFormat.JPEG
                cam.parameters = parameters
            }

        }
        requestLayout()
        setPreviewCallback()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopPreviewAndFreeCamera()
    }

    private fun stopPreviewAndFreeCamera() {
        camera?.let {
            // Call stopPreview() to stop updating the preview surface.
            it.stopPreview()
            it.setPreviewCallback(null)
            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            it.release()
            camera = null
        }
    }

    fun setPreviewCallback() {
        camera?.let {
            it.startPreview()
            it.setPreviewCallback(previewCallback)
        }
    }

    private val previewCallback = PreviewCallback { data, camera ->
        camera?.let {
            try {
                val pictureSize = camera.parameters.previewSize
                Log.d(TAG, "onPreviewFrame - received image " + pictureSize.width + "x" + pictureSize.height)
                val yuv = Mat(Size(pictureSize.width.toDouble(), pictureSize.height * 1.5), CvType.CV_8UC1)
                yuv.put(0, 0, data)
                val mat = Mat(Size(pictureSize.width.toDouble(), pictureSize.height.toDouble()), CvType.CV_8UC4)
                Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2BGR_NV21, 4)
                yuv.release()
                val originalPreviewSize = mat.size()
                val originalPreviewArea = mat.rows() * mat.cols()
                val largestQuad = ScanUtils.detectLargestQuadrilateral(mat)
                clearAndInvalidateCanvas()
                mat.release()
                if (null != largestQuad) {
                    drawLargestRect(largestQuad.contour, largestQuad.points, originalPreviewSize, originalPreviewArea)
                } else {
                    showFindingReceiptHint()
                }
            } catch (e: Exception) {
                showFindingReceiptHint()
            }
        }
    }

    private fun drawLargestRect(approx: MatOfPoint2f, points: Array<Point>, stdSize: Size, previewArea: Int) {
        val path = Path()
        // ATTENTION: axis are swapped
        val previewWidth = stdSize.height.toFloat()
        val previewHeight = stdSize.width.toFloat()
        Log.i(TAG, "previewWidth: $previewWidth")
        Log.i(TAG, "previewHeight: $previewHeight")

        //Points are drawn in anticlockwise direction
        path.moveTo(previewWidth - points[0].y.toFloat(), points[0].x.toFloat())
        path.lineTo(previewWidth - points[1].y.toFloat(), points[1].x.toFloat())
        path.lineTo(previewWidth - points[2].y.toFloat(), points[2].x.toFloat())
        path.lineTo(previewWidth - points[3].y.toFloat(), points[3].x.toFloat())
        path.close()
        val area = Math.abs(Imgproc.contourArea(approx))
        Log.i(TAG, "Contour Area: $area")
        val newBox = PathShape(path, previewWidth, previewHeight)
        val paint = Paint()
        val border = Paint()

        //Height calculated on Y axis
        var resultHeight = points[1].x - points[0].x
        val bottomHeight = points[2].x - points[3].x
        if (bottomHeight > resultHeight) resultHeight = bottomHeight

        //Width calculated on X axis
        var resultWidth = points[3].y - points[0].y
        val bottomWidth = points[2].y - points[1].y
        if (bottomWidth > resultWidth) resultWidth = bottomWidth
        Log.i(TAG, "resultWidth: $resultWidth")
        Log.i(TAG, "resultHeight: $resultHeight")
        val imgDetectionPropsObj = ImageDetectionProperties(previewWidth.toDouble(), previewHeight.toDouble(), resultWidth, resultHeight,
                previewArea.toDouble(), area, points[0], points[1], points[2], points[3])
        val scanHint: ScanHint
        if (imgDetectionPropsObj.isDetectedAreaBeyondLimits) {
            scanHint = ScanHint.FIND_RECT
            cancelAutoCapture()
        } else if (imgDetectionPropsObj.isDetectedAreaBelowLimits) {
            cancelAutoCapture()
            scanHint = if (imgDetectionPropsObj.isEdgeTouching) {
                ScanHint.MOVE_AWAY
            } else {
                ScanHint.MOVE_CLOSER
            }
        } else if (imgDetectionPropsObj.isDetectedHeightAboveLimit) {
            cancelAutoCapture()
            scanHint = ScanHint.MOVE_AWAY
        } else if (imgDetectionPropsObj.isDetectedWidthAboveLimit || imgDetectionPropsObj.isDetectedAreaAboveLimit) {
            cancelAutoCapture()
            scanHint = ScanHint.MOVE_AWAY
        } else {
            if (imgDetectionPropsObj.isEdgeTouching) {
                cancelAutoCapture()
                scanHint = ScanHint.MOVE_AWAY
            } else if (imgDetectionPropsObj.isAngleNotCorrect(approx)) {
                cancelAutoCapture()
                scanHint = ScanHint.ADJUST_ANGLE
            } else {
                Log.i(TAG, "GREEN" + "(resultWidth/resultHeight) > 4: " + resultWidth / resultHeight +
                        " points[0].x == 0 && points[3].x == 0: " + points[0].x + ": " + points[3].x +
                        " points[2].x == previewHeight && points[1].x == previewHeight: " + points[2].x + ": " + points[1].x +
                        "previewHeight: " + previewHeight)
                scanHint = ScanHint.CAPTURING_IMAGE
                clearAndInvalidateCanvas()
                if (!isAutoCaptureScheduled) {
                    scheduleAutoCapture(scanHint)
                }
            }
        }
        Log.i(TAG, "Preview Area 95%: " + 0.95 * previewArea +
                " Preview Area 20%: " + 0.20 * previewArea +
                " Area: " + area.toString() +
                " Label: " + scanHint.toString())
        border.strokeWidth = 12f
        iScanner.displayHint(scanHint)
        setPaintAndBorder(scanHint, paint, border)
        scanCanvasView.clear()
        scanCanvasView.addShape(newBox, paint, border)
        invalidateCanvas()
    }

    private fun scheduleAutoCapture(scanHint: ScanHint) {
        isAutoCaptureScheduled = true
        secondsLeft = 0
        autoCaptureTimer = object : CountDownTimer(2000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if ((millisUntilFinished.toFloat() / 1000.0f).roundToInt() != secondsLeft) {
                    secondsLeft = (millisUntilFinished.toFloat() / 1000.0f).roundToInt()
                }
                Log.v(TAG, "" + millisUntilFinished / 1000)
                when (secondsLeft) {
                    1 -> autoCapture(scanHint)
                    else -> {
                    }
                }
            }

            override fun onFinish() {
                isAutoCaptureScheduled = false
            }
        }
        autoCaptureTimer?.start()
    }

    private fun autoCapture(scanHint: ScanHint) {
        if (isCapturing) return
        if (ScanHint.CAPTURING_IMAGE == scanHint) {
            try {
                isCapturing = true
                iScanner.displayHint(ScanHint.CAPTURING_IMAGE)
                camera?.let {
                    it.takePicture(mShutterCallBack, null, pictureCallback)
                    it.setPreviewCallback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun cancelAutoCapture() {
        if (isAutoCaptureScheduled) {
            isAutoCaptureScheduled = false
            if (null != autoCaptureTimer) {
                autoCaptureTimer!!.cancel()
            }
        }
    }

    private fun showFindingReceiptHint() {
        iScanner.displayHint(ScanHint.FIND_RECT)
        clearAndInvalidateCanvas()
    }

    private fun setPaintAndBorder(scanHint: ScanHint, paint: Paint, border: Paint) {
        var paintColor = 0
        var borderColor = 0
        when (scanHint) {
            ScanHint.MOVE_CLOSER, ScanHint.MOVE_AWAY, ScanHint.ADJUST_ANGLE -> {
                paintColor = Color.argb(30, 255, 38, 0)
                borderColor = Color.rgb(255, 38, 0)
            }
            ScanHint.FIND_RECT -> {
                paintColor = Color.argb(0, 0, 0, 0)
                borderColor = Color.argb(0, 0, 0, 0)
            }
            ScanHint.CAPTURING_IMAGE -> {
                paintColor = Color.argb(30, 38, 216, 76)
                borderColor = Color.rgb(38, 216, 76)
            }
        }
        paint.color = paintColor
        border.color = borderColor
    }

    private val pictureCallback = PictureCallback { data, camera ->
        camera.stopPreview()
        iScanner.displayHint(ScanHint.NO_MESSAGE)
        clearAndInvalidateCanvas()
        var bitmap = ScanUtils.decodeBitmapFromByteArray(data,
                ScanConstants.HIGHER_SAMPLING_THRESHOLD, ScanConstants.HIGHER_SAMPLING_THRESHOLD)
        val matrix = Matrix()
        matrix.postRotate(90f)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        iScanner.onPictureClicked(bitmap)
        postDelayed({ isCapturing = false }, 3000)
    }

    private val mShutterCallBack = ShutterCallback {
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.playSoundEffect(AudioManager.FLAG_PLAY_SOUND)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        vWidth = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        vHeight = View.resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        setMeasuredDimension(vWidth, vHeight)
        previewSize = ScanUtils.getOptimalPreviewSize(camera, vWidth, vHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount > 0) {
            val width = r - l
            val height = b - t
            var previewWidth = width
            var previewHeight = height
            if (previewSize != null) {
                previewWidth = previewSize!!.width
                previewHeight = previewSize!!.height

                (context as? Activity)?.let { act ->
                    val displayOrientation = ScanUtils.configureCameraAngle(act)
                    if (displayOrientation == 90 || displayOrientation == 270) {
                        previewWidth = previewSize!!.height
                        previewHeight = previewSize!!.width
                    }
                    Log.d(TAG, "previewWidth:$previewWidth previewHeight:$previewHeight")
                }
            }
            val nW: Int
            val nH: Int
            val top: Int
            val left: Int
            val scale = 1.0f

            // Center the child SurfaceView within the parent.
            if (width * previewHeight < height * previewWidth) {
                Log.d(TAG, "center horizontally")
                val scaledChildWidth = (previewWidth * height / previewHeight * scale).toInt()
                nW = (width + scaledChildWidth) / 2
                nH = (height * scale).toInt()
                top = 0
                left = (width - scaledChildWidth) / 2
            } else {
                Log.d(TAG, "center vertically")
                val scaledChildHeight = (previewHeight * width / previewWidth * scale).toInt()
                nW = (width * scale).toInt()
                nH = (height + scaledChildHeight) / 2
                top = (height - scaledChildHeight) / 2
                left = 0
            }
            mSurfaceView.layout(left, top, nW, nH)
            scanCanvasView.layout(left, top, nW, nH)
            Log.d("layout", "left:$left")
            Log.d("layout", "top:$top")
            Log.d("layout", "right:$nW")
            Log.d("layout", "bottom:$nH")
        }
    }

    companion object {
        private val TAG = ScanSurfaceView::class.java.simpleName
    }

    init {
        mSurfaceView = SurfaceView(context)
        addView(mSurfaceView)
        scanCanvasView = ScanCanvasView(context)
        addView(scanCanvasView)
        val surfaceHolder = mSurfaceView.holder
        surfaceHolder.addCallback(this)
        this.iScanner = iScanner
    }
}