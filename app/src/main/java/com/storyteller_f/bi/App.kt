package com.storyteller_f.bi

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerRepository
import com.a10miaomiao.bilimiao.comm.delegate.player.entity.SubtitleSourceInfo
import com.a10miaomiao.bilimiao.comm.entity.player.SubtitleJsonInfo
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.storyteller_f.bi.unstable.PlayerDelegate
import com.storyteller_f.bi.unstable.readUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class App : Application() {

    companion object {
        lateinit var commApp: BilimiaoCommApp
    }

    init {
        commApp = BilimiaoCommApp(this)
    }

    override fun onCreate() {
        super.onCreate()
        commApp.onCreate()
        readUserInfo()
    }
}
