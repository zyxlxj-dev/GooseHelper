package com.example.goosehelper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val requestCapture = 4102
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
        }
        status = TextView(this).apply {
            text = "抓大鹅 AI 辅助器 0.1.0\n\n第一阶段：屏幕捕获与悬浮标记框架。\n识别模型需要根据实际游戏截图适配。"
            textSize = 17f
        }
        val overlayButton = Button(this).apply {
            text = "① 授权悬浮窗"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(i)
                } else status.text = "悬浮窗权限已允许。接下来授权屏幕捕获。"
            }
        }
        val startButton = Button(this).apply {
            text = "② 开始屏幕识别"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    status.text = "请先授予悬浮窗权限。"
                    return@setOnClickListener
                }
                val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
            }
        }
        val stopButton = Button(this).apply {
            text = "停止辅助器"
            setOnClickListener { stopService(Intent(this@MainActivity, CaptureService::class.java)); status.text = "辅助器已停止。" }
        }
        layout.addView(status); layout.addView(overlayButton); layout.addView(startButton); layout.addView(stopButton)
        setContentView(layout)
    }

    @Deprecated("Deprecated by Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCapture && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, CaptureService::class.java).apply {
                putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CaptureService.EXTRA_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
            status.text = "屏幕捕获已授权。识别引擎等待游戏截图适配。"
        }
    }
}
