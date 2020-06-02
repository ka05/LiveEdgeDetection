package com.adityaarora.liveedgedetection.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.adityaarora.liveedgedetection.R
import com.adityaarora.liveedgedetection.activity.ScanActivity
import com.adityaarora.liveedgedetection.util.ScanUtils
import java.util.ArrayList
import java.util.HashMap
import kotlin.math.abs

/**
 * This class defines polygon for cropping
 */
class PolygonView : FrameLayout {

    private lateinit var paint: Paint
    private lateinit var pointer1: ImageView
    private lateinit var pointer2: ImageView
    private lateinit var pointer3: ImageView
    private lateinit var pointer4: ImageView
    private lateinit var midPointer13: ImageView
    private lateinit var midPointer12: ImageView
    private lateinit var midPointer34: ImageView
    private lateinit var midPointer24: ImageView
    private lateinit var polygonView: PolygonView
    private lateinit var circleFillPaint: Paint

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        polygonView = this
        pointer1 = getImageView(0, 0)
        pointer2 = getImageView(width, 0)
        pointer3 = getImageView(0, height)
        pointer4 = getImageView(width, height)
        midPointer13 = getImageViewTransparent(0, height / 2)
        midPointer13.setOnTouchListener(MidPointTouchListenerImpl(pointer1, pointer3))
        midPointer12 = getImageViewTransparent(0, width / 2)
        midPointer12.setOnTouchListener(MidPointTouchListenerImpl(pointer1, pointer2))
        midPointer34 = getImageViewTransparent(0, width / 2)
        midPointer34.setOnTouchListener(MidPointTouchListenerImpl(pointer3, pointer4))
        midPointer24 = getImageViewTransparent(0, height / 2)
        midPointer24.setOnTouchListener(MidPointTouchListenerImpl(pointer2, pointer4))
        addView(pointer1)
        addView(pointer2)
        addView(midPointer13)
        addView(midPointer12)
        addView(midPointer34)
        addView(midPointer24)
        addView(pointer3)
        addView(pointer4)
        initPaint()
    }

    override fun attachViewToParent(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.attachViewToParent(child, index, params)
    }

    private fun initPaint() {
        paint = Paint()
        paint.color = resources.getColor(R.color.crop_color)
        paint.strokeWidth = 7f
        paint.isAntiAlias = true
        circleFillPaint = Paint()
        circleFillPaint.style = Paint.Style.FILL
        circleFillPaint.color = resources.getColor(R.color.crop_color)
        circleFillPaint.isAntiAlias = true
    }

    var points: Map<Int, PointF>
        get() {
            val points: MutableList<PointF> = ArrayList()
            points.add(PointF(pointer1.x, pointer1.y))
            points.add(PointF(pointer2.x, pointer2.y))
            points.add(PointF(pointer3.x, pointer3.y))
            points.add(PointF(pointer4.x, pointer4.y))
            return getOrderedPoints(points)
        }
        set(pointFMap) {
            if (pointFMap.size == 4) {
                setPointsCoordinates(pointFMap)
            }
        }

    private fun getOrderedPoints(points: List<PointF>): Map<Int, PointF> {
        val centerPoint = PointF()
        val size = points.size
        for (pointF in points) {
            centerPoint.x += pointF.x / size
            centerPoint.y += pointF.y / size
        }
        val orderedPoints: MutableMap<Int, PointF> = HashMap()
        for (pointF in points) {
            var index = -1
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3
            }
            orderedPoints[index] = pointF
        }
        return orderedPoints
    }

    private fun setPointsCoordinates(pointFMap: Map<Int, PointF>) {
        pointer1.x = pointFMap[0]!!.x
        pointer1.y = pointFMap[0]!!.y
        pointer2.x = pointFMap[1]!!.x
        pointer2.y = pointFMap[1]!!.y
        pointer3.x = pointFMap[2]!!.x
        pointer3.y = pointFMap[2]!!.y
        pointer4.x = pointFMap[3]!!.x
        pointer4.y = pointFMap[3]!!.y
        midPointer13.x = pointer3.x - (pointer3.x - pointer1.x) / 2
        midPointer13.y = pointer3.y - (pointer3.y - pointer1.y) / 2
        midPointer24.x = pointer4.x - (pointer4.x - pointer2.x) / 2
        midPointer24.y = pointer4.y - (pointer4.y - pointer2.y) / 2
        midPointer34.x = pointer4.x - (pointer4.x - pointer3.x) / 2
        midPointer34.y = pointer4.y - (pointer4.y - pointer3.y) / 2
        midPointer12.x = pointer2.x - (pointer2.x - pointer1.x) / 2
        midPointer12.y = pointer2.y - (pointer2.y - pointer1.y) / 2
    }

    fun resetPoints(polygonPoints: PolygonPoints) {
        Log.v(TAG, """
     P1:${pointer1.x},${pointer1.y}
     P2:${pointer2.x},${pointer2.y}
     P3:${pointer3.x},${pointer3.y}
     P4:${pointer4.x},${pointer4.y}
     """.trimIndent())
        pointer1.x = polygonPoints.topLeftPoint.x
        pointer1.y = polygonPoints.topLeftPoint.y
        pointer2.x = polygonPoints.topRightPoint.x
        pointer2.y = polygonPoints.topRightPoint.y
        pointer3.x = polygonPoints.bottomLeftPoint.x
        pointer3.y = polygonPoints.bottomLeftPoint.y
        pointer4.x = polygonPoints.bottomRightPoint.x
        pointer4.y = polygonPoints.bottomRightPoint.y
        polygonView.invalidate()
        Log.v(TAG, """
     P1:${pointer1.x},${pointer1.y}
     P2:${pointer2.x},${pointer2.y}
     P3:${pointer3.x},${pointer3.y}
     P4:${pointer4.x},${pointer4.y}
     """.trimIndent())
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val bgPaint = Paint()
        bgPaint.color = resources.getColor(R.color.colorBlackThirtyFivePercentAlpha)
        bgPaint.isAntiAlias = true
        val path1 = drawOutTopRect(canvas)
        canvas.drawPath(path1, bgPaint)
        val path4 = drawOutMiddleLeftRect()
        canvas.drawPath(path4, bgPaint)
        val path5 = drawOutMiddleRightRect(canvas)
        canvas.drawPath(path5, bgPaint)
        val path6 = drawOutBottomRect(canvas)
        canvas.drawPath(path6, bgPaint)
        canvas.drawLine(pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2, pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2, paint)
        canvas.drawLine(pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2, pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2, paint)
        canvas.drawLine(pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2, pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2, paint)
        canvas.drawLine(pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2, pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2, paint)
        midPointer13.x = pointer3.x - (pointer3.x - pointer1.x) / 2
        midPointer13.y = pointer3.y - (pointer3.y - pointer1.y) / 2
        midPointer24.x = pointer4.x - (pointer4.x - pointer2.x) / 2
        midPointer24.y = pointer4.y - (pointer4.y - pointer2.y) / 2
        midPointer34.x = pointer4.x - (pointer4.x - pointer3.x) / 2
        midPointer34.y = pointer4.y - (pointer4.y - pointer3.y) / 2
        midPointer12.x = pointer2.x - (pointer2.x - pointer1.x) / 2
        midPointer12.y = pointer2.y - (pointer2.y - pointer1.y) / 2
        val radius = ScanUtils.dp2px(context, 11f)
        canvas.drawCircle(pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2, radius.toFloat(), circleFillPaint)
        canvas.drawCircle(pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2, radius.toFloat(), circleFillPaint)
        canvas.drawCircle(pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2, radius.toFloat(), circleFillPaint)
        canvas.drawCircle(pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2, radius.toFloat(), circleFillPaint)
        canvas.drawCircle(midPointer13.x + midPointer13.width / 2, midPointer13.y + midPointer13.height / 2, radius.toFloat(), circleFillPaint)
        canvas.drawCircle(midPointer24.x + midPointer24.width / 2, midPointer24.y + midPointer24.height / 2, radius.toFloat(), circleFillPaint)
        canvas.drawCircle(midPointer34.x + midPointer34.width / 2, midPointer34.y + midPointer34.height / 2, radius.toFloat(), circleFillPaint)
        canvas.drawCircle(midPointer12.x + midPointer12.width / 2, midPointer12.y + midPointer12.height / 2, radius.toFloat(), circleFillPaint)
    }

    private fun drawOutBottomRect(canvas: Canvas): Path {
        val path = Path()
        path.moveTo(0f, canvas.height.toFloat())
        path.lineTo(canvas.width.toFloat(), canvas.height.toFloat())
        path.lineTo(canvas.width.toFloat(), pointer4.y + pointer4.height / 2)
        path.lineTo(pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2)
        path.lineTo(pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2)
        path.lineTo(0f, pointer3.y + pointer3.height / 2)
        path.close()
        return path
    }

    private fun drawOutMiddleRightRect(canvas: Canvas): Path {
        val path = Path()
        path.moveTo(pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2)
        path.lineTo(canvas.width.toFloat(), pointer2.y + pointer2.height / 2)
        path.lineTo(canvas.width.toFloat(), pointer4.y + pointer4.height / 2)
        path.lineTo(pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2)
        path.close()
        return path
    }

    private fun drawOutMiddleLeftRect(): Path {
        val path = Path()
        path.moveTo(0f, pointer1.y + pointer1.height / 2)
        path.lineTo(pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2)
        path.lineTo(pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2)
        path.lineTo(0f, pointer3.y + pointer3.height / 2)
        path.close()
        return path
    }

    private fun drawOutTopRect(canvas: Canvas): Path {
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(canvas.width.toFloat(), 0f)
        path.lineTo(canvas.width.toFloat(), pointer2.y + pointer2.height / 2)
        path.lineTo(pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2)
        path.lineTo(pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2)
        path.lineTo(0f, pointer1.y + pointer1.height / 2)
        path.close()
        return path
    }

    private fun getImageView(x: Int, y: Int): ImageView {
        val imageView = ImageView(context)
        val layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.layoutParams = layoutParams
        imageView.setImageResource(R.drawable.circle)
        imageView.x = x.toFloat()
        imageView.y = y.toFloat()
        imageView.setOnTouchListener(TouchListenerImpl())
        return imageView
    }

    private fun getImageViewTransparent(x: Int, y: Int): ImageView {
        val imageView = ImageView(context)
        val layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.layoutParams = layoutParams
        imageView.setImageResource(R.drawable.circle)
        imageView.x = x.toFloat()
        imageView.y = y.toFloat()
        //        imageView.setOnTouchListener(new MidPointTouchListenerImpl());
        return imageView
    }

    private inner class MidPointTouchListenerImpl(
            private val mainPointer1: ImageView,
            private val mainPointer2: ImageView
    ) : OnTouchListener {
        val downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'
        var latestPoint = PointF()
        var latestPoint1 = PointF()
        var latestPoint2 = PointF()
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val eid = event.action
            when (eid) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (abs(mainPointer1.x - mainPointer2.x) > abs(mainPointer1.y - mainPointer2.y)) {
                        if (mainPointer2.y + mv.y + v.height < polygonView.height && mainPointer2.y + mv.y > 0) {
                            v.x = (startPT.y + mv.y)
                            startPT = PointF(v.x, v.y)
                            mainPointer2.y = (mainPointer2.y + mv.y)
                        }
                        if (mainPointer1.y + mv.y + v.height < polygonView.height && mainPointer1.y + mv.y > 0) {
                            v.x = (startPT.y + mv.y)
                            startPT = PointF(v.x, v.y)
                            mainPointer1.y = (mainPointer1.y + mv.y)
                        }
                    } else {
                        if (mainPointer2.x + mv.x + v.width < polygonView.width && mainPointer2.x + mv.x > 0) {
                            v.x = (startPT.x + mv.x)
                            startPT = PointF(v.x, v.y)
                            mainPointer2.x = (mainPointer2.x + mv.x)
                        }
                        if (mainPointer1.x + mv.x + v.width < polygonView.width && mainPointer1.x + mv.x > 0) {
                            v.x = (startPT.x + mv.x)
                            startPT = PointF(v.x, v.y)
                            mainPointer1.x = (mainPointer1.x + mv.x)
                        }
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    ScanActivity.allDraggedPointsStack.push(PolygonPoints(PointF(pointer1.x, pointer1.y),
                            PointF(pointer2.x, pointer2.y),
                            PointF(pointer3.x, pointer3.y),
                            PointF(pointer4.x, pointer4.y)))
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(v.x, v.y)
                    latestPoint = PointF(v.x, v.y)
                    latestPoint1 = PointF(mainPointer1.x, mainPointer1.y)
                    latestPoint2 = PointF(mainPointer2.x, mainPointer2.y)
                }
                MotionEvent.ACTION_UP -> {
                    var color = 0
                    if (isValidShape(points) && isValidPointer1 && isValidPointer2 && isValidPointer3 && isValidPointer4) {
                        color = resources.getColor(R.color.crop_color)
                        latestPoint.x = v.x
                        latestPoint.y = v.y
                        latestPoint1.x = mainPointer1.x
                        latestPoint1.y = mainPointer1.y
                        latestPoint2.x = mainPointer2.x
                        latestPoint2.y = mainPointer2.y
                    } else {
                        ScanActivity.allDraggedPointsStack.pop()
                        color = resources.getColor(R.color.crop_color)
                        v.x = latestPoint.x
                        v.y = latestPoint.y
                        mainPointer1.x = latestPoint1.x
                        mainPointer1.y = latestPoint1.y
                        mainPointer2.x = latestPoint2.x
                        mainPointer2.y = latestPoint2.y
                    }
                    paint.color = color
                }
                else -> {
                }
            }
            polygonView.invalidate()
            return true
        }

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }

    private fun isValidShape(pointFMap: Map<Int, PointF>): Boolean {
        return pointFMap.size == 4
    }

    private val isValidPointer4: Boolean
        private get() = pointer4.y > pointer2.y && pointer4.x > pointer3.x

    private val isValidPointer3: Boolean
        private get() = pointer3.y > pointer1.y && pointer3.x < pointer4.x

    private val isValidPointer2: Boolean
        private get() = pointer2.y < pointer4.y && pointer2.x > pointer1.x

    private val isValidPointer1: Boolean
        private get() = pointer1.y < pointer3.y && pointer1.x < pointer2.x

    private val isValidMidPointerX: Boolean
        private get() = midPointer24.x > midPointer13.x

    private val isValidMidPointerY: Boolean
        private get() = midPointer34.y > midPointer12.y

    private inner class TouchListenerImpl : OnTouchListener {
        val downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'
        var latestPoint = PointF()
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val eid = event.action
            when (eid) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (startPT.x + mv.x + v.width < polygonView.width && startPT.y + mv.y + v.height < polygonView.height && startPT.x + mv.x > 0 && startPT.y + mv.y > 0) {
                        v.x = (startPT.x + mv.x)
                        v.y = (startPT.y + mv.y)
                        startPT = PointF(v.x, v.y)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    ScanActivity.allDraggedPointsStack.push(PolygonPoints(PointF(pointer1.x, pointer1.y),
                            PointF(pointer2.x, pointer2.y),
                            PointF(pointer3.x, pointer3.y),
                            PointF(pointer4.x, pointer4.y)))
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(v.x, v.y)
                    latestPoint = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_UP -> {
                    var color = 0
                    if (isValidShape(points) && isValidPointer4 && isValidPointer3 && isValidPointer2 && isValidPointer1) {
                        color = resources.getColor(R.color.crop_color)
                        latestPoint.x = v.x
                        latestPoint.y = v.y
                    } else {
                        ScanActivity.allDraggedPointsStack.pop()
                        color = resources.getColor(R.color.crop_color)
                        v.x = latestPoint.x
                        v.y = latestPoint.y
                    }
                    paint.color = color
                }
                else -> {
                }
            }
            polygonView.invalidate()
            return true
        }
    }

    companion object {
        private val TAG = PolygonView::class.java.simpleName
    }
}