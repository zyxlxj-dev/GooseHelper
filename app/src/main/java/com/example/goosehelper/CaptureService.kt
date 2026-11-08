package com.example.goosehelper

import android.app.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager

class CaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        private const val CHANNEL = "goose_helper_capture"
    }
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var overlay: MarkerOverlay? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("抓大鹅 AI 辅助器运行中")
            .setContentText("屏幕识别服务已启动；不会自动点击游戏。")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
        startForeground(7, notification)
        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data)
        startCapture()
        showOverlay()
        return START_NOT_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val w = metrics.widthPixels; val h = metrics.heightPixels; val density = metrics.densityDpi
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        reader?.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * image.width
                val full = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
                full.copyPixelsFromBuffer(buffer)
                val frame = Bitmap.createBitmap(full, 0, 0, image.width, image.height)
                full.recycle()
                // TODO: Pass frame into the game-specific item detector and update overlay markers.
                frame.recycle()
            } finally { image.close() }
        }, null)
        display = projection?.createVirtualDisplay("GooseHelper", w, h, density, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null)
    }

    private fun showOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlay = MarkerOverlay(this)
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
        wm.addView(overlay, params)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "屏幕识别", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onDestroy() {
        try { overlay?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } } catch (_: Exception) {}
        display?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
