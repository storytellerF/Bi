package com.storyteller_f.bi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.storyteller_f.bi.components.VideoPage
import com.storyteller_f.bi.ui.theme.BiTheme


class VideoActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoId = intent.getStringExtra("videoId")!!
        setContent {
            BiTheme {
                Surface {
                    VideoPage(videoId)
                }
            }
        }
    }
}