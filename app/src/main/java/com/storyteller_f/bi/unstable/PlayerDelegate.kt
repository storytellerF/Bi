package com.storyteller_f.bi.unstable

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerRepository
import com.a10miaomiao.bilimiao.comm.delegate.player.entity.SubtitleSourceInfo
import com.a10miaomiao.bilimiao.comm.entity.player.SubtitleJsonInfo
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlayerDelegate {
    suspend fun mediaSource(context: Context, source: BasePlayerRepository): MediaSource {
        val dataSource = source.getPlayerUrl(64, 1).url
        val dataSourceArr = dataSource.split("\n")
        val subtitleMediaSources = source.subtitleMediaSources(context)
        val header = getDefaultRequestProperties(source)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        dataSourceFactory.setUserAgent(DEFAULT_USER_AGENT)
        dataSourceFactory.setDefaultRequestProperties(header)
        when (dataSourceArr[0]) {
            "[local-merging]" -> {
                // 本地音视频分离
                val localSourceFactory = DefaultDataSource.Factory(context)
                val videoMedia = MediaItem.fromUri(dataSourceArr[1])
                val audioMedia = MediaItem.fromUri(dataSourceArr[2])
                return MergingMediaSource(
                    ProgressiveMediaSource.Factory(localSourceFactory)
                        .createMediaSource(videoMedia),
                    ProgressiveMediaSource.Factory(localSourceFactory)
                        .createMediaSource(audioMedia)
                )
            }

            "[merging]" -> {
                // 音视频分离
                val videoMedia = MediaItem.fromUri(dataSourceArr[1])
                val audioMedia = MediaItem.fromUri(dataSourceArr[2])
                return MergingMediaSource(
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(videoMedia),
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(audioMedia),
                    *subtitleMediaSources.toTypedArray()
                )
            }

            "[concatenating]" -> {
                // 视频拼接
                val mediaSource = ConcatenatingMediaSource().apply {
                    for (i in 1 until dataSourceArr.size) {
                        addMediaSource(
                            ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(dataSourceArr[i]))
                        )
                    }
                }
                if (subtitleMediaSources.isNotEmpty()) return MergingMediaSource(mediaSource, *subtitleMediaSources.toTypedArray())
                return mediaSource
            }

            "[dash-mpd]" -> {
                // Create a data source factory.
                // Create a DASH media source pointing to a DASH manifest uri.
                val uri = Uri.parse(dataSourceArr[1])
                val dashStr = dataSourceArr[2]
                val dashManifest =
                    DashManifestParser().parse(uri, dashStr.toByteArray().inputStream())
                return DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(dashManifest)
            }

            else -> {
                throw Exception("not support ${dataSourceArr[0]}")
            }
        }
    }

    private fun getDefaultRequestProperties(playerSource: BasePlayerRepository): Map<String, String> {
        val header = HashMap<String, String>()
        if (playerSource is VideoPlayerRepository) {
            header["Referer"] = DEFAULT_REFERER
        }
        header["User-Agent"] = DEFAULT_USER_AGENT
        return header
    }

    companion object {
        const val DEFAULT_REFERER = "https://www.bilibili.com/"
        const val DEFAULT_USER_AGENT = "Bilibili Freedoooooom/MarkII"
        fun formatDuration(duration: Double): String {
            val toLong = duration.toLong()

            val seconds = toLong % 60
            val minutes = toLong / 60
            val hours = toLong / (60 * 60)
            val millis = ((duration - toLong) * 1000).toInt()

            return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
        }
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
        } && parentFile != null && (parentFile.exists() || parentFile.mkdirs()) && (file.exists() || withContext(
            Dispatchers.IO) {
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
