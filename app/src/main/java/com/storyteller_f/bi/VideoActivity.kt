package com.storyteller_f.bi

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.storyteller_f.bi.components.BangumiPage
import com.storyteller_f.bi.components.VideoPage
import com.storyteller_f.bi.ui.theme.BiTheme


class VideoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")
        val videoId = intent.getStringExtra("videoId")!!
        val progress = intent.getLongExtra("progress", 0L)
        val business = intent.getStringExtra("business")!!
        val extra = intent.getStringExtra("extra")!!
        setContent {
            BiTheme {
                Surface {
                    if (business == "archive") {
                        VideoPage(videoId, progress) {
                            requestedOrientation =
                                if (it) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    } else {
                        BangumiPage(videoId, extra)
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged() called with: newConfig = $newConfig")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")
    }

    companion object {
        private const val TAG = "VideoActivity"
    }
}

fun Context.playVideo(kid: String?, oid: String?, business: String, progress: Long = 0L) {
    startActivity(
        Intent(
            this,
            VideoActivity::class.java
        ).apply {
            putExtra("videoId", oid)
            putExtra("extra", kid)
            putExtra("progress", progress)
            putExtra("business", business)
        })
}