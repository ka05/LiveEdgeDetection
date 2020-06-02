package com.adityaarora.liveedgedetection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.adityaarora.liveedgedetection.activity.ScanActivity
import com.adityaarora.liveedgedetection.constants.ScanConstants
import com.adityaarora.liveedgedetection.util.ScanUtils

class MainActivity : AppCompatActivity() {

    private var scannedImageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scannedImageView = findViewById(R.id.scanned_image)
        startScan()
    }

    private fun startScan() {
        val intent = Intent(this, ScanActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (null != data && null != data.extras) {
                    val filePath = data.extras!!.getString(ScanConstants.SCANNED_RESULT)
                    val baseBitmap = ScanUtils.decodeBitmapFromFile(filePath, ScanConstants.IMAGE_NAME)
                    scannedImageView?.scaleType = ImageView.ScaleType.FIT_CENTER
                    scannedImageView?.setImageBitmap(baseBitmap)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 101
    }
}