package com.adityaarora.liveedgedetection.view

import android.graphics.PointF

/**
 * This class holds polygon coordinates
 */
class PolygonPoints(
        val topLeftPoint: PointF,
        val topRightPoint: PointF,
        val bottomLeftPoint: PointF,
        val bottomRightPoint: PointF
)