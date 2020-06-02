package com.adityaarora.liveedgedetection.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.adityaarora.liveedgedetection.R
import com.adityaarora.liveedgedetection.activity.ScanActivity
import com.adityaarora.liveedgedetection.constants.ScanConstants
import com.adityaarora.liveedgedetection.enums.ScanHint
import com.adityaarora.liveedgedetection.interfaces.IScanner
import com.adityaarora.liveedgedetection.util.ScanUtils
import com.adityaarora.liveedgedetection.util.permissions.PermissionsDelegate
import com.adityaarora.liveedgedetection.util.permissions.PermissionsHandler
import com.adityaarora.liveedgedetection.util.permissions.PermissionsHandlerImpl
import com.adityaarora.liveedgedetection.view.PolygonPoints
import com.adityaarora.liveedgedetection.view.PolygonView
import com.adityaarora.liveedgedetection.view.ProgressDialogFragment
import com.adityaarora.liveedgedetection.view.ScanSurfaceView
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.util.ArrayList
import java.util.HashMap
import java.util.Stack
import kotlin.math.abs

/**
 * This class initiates camera and detects edges on live view
 */
class ScanActivity : AppCompatActivity(), IScanner, View.OnClickListener, PermissionsDelegate {

    private var containerScan: ViewGroup? = null
    private var cameraPreviewLayout: FrameLayout? = null
    private var mImageSurfaceView: ScanSurfaceView? = null
    private var captureHintText: TextView? = null
    private var captureHintLayout: LinearLayout? = null
    private var polygonView: PolygonView? = null
    private var cropImageView: ImageView? = null
    private var cropAcceptBtn: View? = null
    private var cropRejectBtn: View? = null
    private var copyBitmap: Bitmap? = null
    private var cropLayout: FrameLayout? = null

    private val permissionsHandler: PermissionsHandler =
            PermissionsHandlerImpl(this)

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        init()
    }

    private fun init() {
        containerScan = findViewById(R.id.container_scan)
        cameraPreviewLayout = findViewById(R.id.camera_preview)
        captureHintLayout = findViewById(R.id.capture_hint_layout)
        captureHintText = findViewById(R.id.capture_hint_text)
        polygonView = findViewById(R.id.polygon_view)
        cropImageView = findViewById(R.id.crop_image_view)
        cropAcceptBtn = findViewById(R.id.crop_accept_btn)
        cropRejectBtn = findViewById(R.id.crop_reject_btn)
        cropLayout = findViewById(R.id.crop_layout)
        cropAcceptBtn?.setOnClickListener(this)
        cropRejectBtn?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                TransitionManager.beginDelayedTransition(containerScan)
            }
            cropLayout?.visibility = View.GONE
            mImageSurfaceView?.setPreviewCallback()
        }
        checkCameraPermissions()
    }

    private fun checkCameraPermissions() {
        if (!permissionsHandler.hasPermissions(this, Manifest.permission.CAMERA)) {
            permissionsHandler.requestPermissions(this, Manifest.permission.CAMERA)
        } else {
            loadCameraView()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHandler.onRequestPermissionsResult(this, Manifest.permission.CAMERA, requestCode, grantResults)
    }

    override fun displayHint(scanHint: ScanHint?) {
        captureHintLayout?.visibility = if (scanHint == ScanHint.NO_MESSAGE) View.GONE else View.VISIBLE
        captureHintText?.text = scanHint?.getText(this)
        captureHintLayout?.background = scanHint?.getDrawable(this)
    }

    override fun onPictureClicked(bitmap: Bitmap?) {
        try {
            copyBitmap = bitmap!!.copy(Bitmap.Config.ARGB_8888, true)
            val height = window.findViewById<View>(Window.ID_ANDROID_CONTENT).height
            val width = window.findViewById<View>(Window.ID_ANDROID_CONTENT).width

            if (copyBitmap != null) {

                copyBitmap = ScanUtils.resizeToScreenContentSize(copyBitmap!!, width, height)
                val originalMat = Mat(copyBitmap!!.height, copyBitmap!!.width, CvType.CV_8UC1)
                Utils.bitmapToMat(copyBitmap, originalMat)
                val points: ArrayList<PointF>
                val pointFs: MutableMap<Int, PointF> = HashMap()
                try {
                    val quad = ScanUtils.detectLargestQuadrilateral(originalMat)
                    if (null != quad) {
                        val resultArea = abs(Imgproc.contourArea(quad.contour))
                        val previewArea = originalMat.rows() * originalMat.cols().toDouble()
                        if (resultArea > previewArea * 0.08) {
                            points = ArrayList()
                            points.add(PointF(quad.points[0].x.toFloat(), quad.points[0].y.toFloat()))
                            points.add(PointF(quad.points[1].x.toFloat(), quad.points[1].y.toFloat()))
                            points.add(PointF(quad.points[3].x.toFloat(), quad.points[3].y.toFloat()))
                            points.add(PointF(quad.points[2].x.toFloat(), quad.points[2].y.toFloat()))
                        } else {
                            points = ScanUtils.getPolygonDefaultPoints(copyBitmap!!)
                        }
                    } else {
                        points = ScanUtils.getPolygonDefaultPoints(copyBitmap!!)
                    }
                    var index = -1
                    for (pointF in points) {
                        pointFs[++index] = pointF
                    }
                    polygonView!!.points = pointFs
                    val padding = resources.getDimension(R.dimen.scan_padding).toInt()
                    val layoutParams = FrameLayout.LayoutParams(copyBitmap!!.width + 2 * padding, copyBitmap!!.height + 2 * padding)
                    layoutParams.gravity = Gravity.CENTER
                    polygonView!!.layoutParams = layoutParams
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) TransitionManager.beginDelayedTransition(containerScan)
                    cropLayout!!.visibility = View.VISIBLE
                    cropImageView!!.setImageBitmap(copyBitmap)
                    cropImageView!!.scaleType = ImageView.ScaleType.FIT_XY
                } catch (e: Exception) {
                    Log.e(TAG, e.message, e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }

    @Synchronized
    private fun showProgressDialog(message: String) {
        if (progressDialogFragment != null && progressDialogFragment!!.isVisible) {
            // Before creating another loading dialog, close all opened loading dialogs (if any)
            progressDialogFragment?.dismissAllowingStateLoss()
        }
        progressDialogFragment = null
        progressDialogFragment = ProgressDialogFragment(message)
        val fm = fragmentManager
        progressDialogFragment?.show(fm, ProgressDialogFragment::class.java.toString())
    }

    @Synchronized
    private fun dismissDialog() {
        progressDialogFragment?.dismissAllowingStateLoss()
    }

    companion object {
        private val TAG = ScanActivity::class.java.simpleName
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 101
        private const val mOpenCvLibrary = "opencv_java3"
        private var progressDialogFragment: ProgressDialogFragment? = null
        val allDraggedPointsStack = Stack<PolygonPoints>()

        init {
            System.loadLibrary(mOpenCvLibrary)
        }
    }

    override fun onClick(view: View) {
        polygonView?.points?.let { points ->

            val croppedBitmap: Bitmap? = if (ScanUtils.isScanPointsValid(points)) {
                val point1 = Point(points[0]!!.x.toDouble(), points[0]!!.y.toDouble())
                val point2 = Point(points[1]!!.x.toDouble(), points[1]!!.y.toDouble())
                val point3 = Point(points[2]!!.x.toDouble(), points[2]!!.y.toDouble())
                val point4 = Point(points[3]!!.x.toDouble(), points[3]!!.y.toDouble())
                ScanUtils.enhanceReceipt(copyBitmap!!, point1, point2, point3, point4)
            } else {
                copyBitmap
            }
            croppedBitmap?.let {
                val path = ScanUtils.saveToInternalMemory(
                        croppedBitmap,
                        ScanConstants.IMAGE_DIR,
                        ScanConstants.IMAGE_NAME,
                        this@ScanActivity,
                        90
                )[0]
                setResult(Activity.RESULT_OK, Intent().putExtra(ScanConstants.SCANNED_RESULT, path))
                //bitmap.recycle();
                System.gc() // This is bad practice! We should be handling memory not forcing GC
                finish()
            }
        }
    }

    private fun loadCameraView() {
        mImageSurfaceView = ScanSurfaceView(this@ScanActivity, this)
        cameraPreviewLayout?.addView(mImageSurfaceView)
    }

    override fun permissionsAccepted() {
        loadCameraView()
    }

    override fun permissionsDenied() {
        // show rational for camera permissions
        // If permissions denied there's nothing we can do
        Toast.makeText(this, getString(R.string.camera_activity_permission_denied_toast), Toast.LENGTH_SHORT).show()
        finish()
    }
}