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

suspend fun BasePlayerRepository.subtitleMediaSources(
    context: Context
): List<SingleSampleMediaSource> {
    return subtitleConfigurations(context).map {
        SingleSampleMediaSource.Factory(DefaultDataSource.Factory(context))
            .createMediaSource(it, C.TIME_UNSET)
    }
}

suspend fun BasePlayerRepository.subtitleConfigurations(
    context: Context
): List<MediaItem.SubtitleConfiguration> {

    val subtitles = getSubtitles()

    val pairs = subtitles.mapNotNull { info: SubtitleSourceInfo ->
        val file = File(context.cacheDir, "/subtitle/$id/${info.lan}.srt")
        val parent = file.parentFile
        when {
            parent == null -> null
            !parent.exists() && !parent.mkdirs() -> null
            file.exists() -> file to info to null
            !file.createNewFile() -> null
            else -> {
                val res = MiaoHttp.request {
                    url = UrlUtil.autoHttps(info.subtitle_url)
                }.awaitCall().gson<SubtitleJsonInfo>()
                file to info to res
            }
        }
    }
    val lanList = pairs.map { it.first.second.lan }
    val infoList = pairs.mapNotNull {
        it.second
    }

    val configurations = pairs.map { (it, jInfo) ->
        val (file, info) = it
        if (jInfo != null) {
            writeToFile(jInfo, file)
        }
        MediaItem.SubtitleConfiguration.Builder(file.toUri())
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .setLabel(info.lan_doc)
            .setId(info.id)
            .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
            .setLanguage(info.lan).build()
    }
    val file = File(context.cacheDir, "subtitle/$id/mix.srt")
    val parentFile = file.parentFile
    if (pairs.size == 2 && infoList.size == 2 && subtitles.size == 2 && lanList.any {
            it.contains("zh")
        } && lanList.any {
            it.contains(
                "en"
            )
        } && parentFile != null && (parentFile.exists() || parentFile.mkdirs()) && (file.exists() || withContext(Dispatchers.IO) {
            file.createNewFile()
        })
    ) {
        val first = infoList.first()
        val jsonInfo = infoList.last()

        writeToFile(first, file) { it, i ->
            "${it.content}\n${jsonInfo.body[i].content}"
        }
        return configurations + MediaItem.SubtitleConfiguration.Builder(file.toUri())
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .setLabel("mix")
            .setRoleFlags(C.ROLE_FLAG_SUBTITLE).build()
    }
    return configurations
}

private suspend fun writeToFile(
    jInfo: SubtitleJsonInfo,
    file: File,
    c: (SubtitleJsonInfo.ItemInfo, Int) -> String = { it, _ -> it.content }
) {
    val content = jInfo.body.mapIndexed { index, itemInfo ->
        val toDuration = PlayerDelegate.formatDuration(
            itemInfo.to
        )
        val fromDuration = PlayerDelegate.formatDuration(itemInfo.from)
        """
${index + 1}
$fromDuration --> $toDuration
${c(itemInfo, index)}
        """.trimIndent()
    }.joinToString("\n\n")
    withContext(Dispatchers.IO) {
        file.writeText(content)
    }
}
