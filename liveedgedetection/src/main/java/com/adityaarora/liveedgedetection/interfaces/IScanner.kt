package com.adityaarora.liveedgedetection.interfaces

import android.graphics.Bitmap
import com.adityaarora.liveedgedetection.enums.ScanHint

/**
 * Interface between activity and surface view
 */
interface IScanner {
    fun displayHint(scanHint: ScanHint?)
    fun onPictureClicked(bitmap: Bitmap?)
}