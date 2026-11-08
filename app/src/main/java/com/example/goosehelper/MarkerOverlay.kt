package com.example.goosehelper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Transparent, non-interactive overlay. Coordinates are screen pixels. */
class MarkerOverlay(context: Context) : View(context) {
    data class Marker(val centerX: Float, val centerY: Float, val radius: Float, val groupSize: Int = 3)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = 0xFF20C66B.toInt()
    }
    private var markers: List<Marker> = emptyList()
    fun setMarkers(next: List<Marker>) { markers = next; postInvalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        markers.forEach { m ->
            canvas.drawCircle(m.centerX, m.centerY, m.radius, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 30f
            canvas.drawText("${m.groupSize}", m.centerX + m.radius * .55f, m.centerY - m.radius * .55f, paint)
            paint.style = Paint.Style.STROKE
        }
    }
}
