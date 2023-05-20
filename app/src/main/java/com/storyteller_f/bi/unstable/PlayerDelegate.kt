package com.storyteller_f.bi.unstable

import android.content.Context
import android.net.Uri
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerSource
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

class PlayerDelegate {
    suspend fun mediaSource(context: Context, source: BasePlayerSource): MediaSource {
        val dataSource = source.getPlayerUrl(64, 1).url
        val dataSourceArr = dataSource.split("\n")
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
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                val header = getDefaultRequestProperties(source)
                dataSourceFactory.setUserAgent(DEFAULT_USER_AGENT)
                dataSourceFactory.setDefaultRequestProperties(header)
                val videoMedia = MediaItem.fromUri(dataSourceArr[1])
                val audioMedia = MediaItem.fromUri(dataSourceArr[2])
                return MergingMediaSource(
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(videoMedia),
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(audioMedia)
                )
            }
            "[concatenating]" -> {
                // 视频拼接
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                val header = getDefaultRequestProperties(source)
                dataSourceFactory.setUserAgent(DEFAULT_USER_AGENT)
                dataSourceFactory.setDefaultRequestProperties(header)
                return ConcatenatingMediaSource().apply {
                    for (i in 1 until dataSourceArr.size) {
                        addMediaSource(
                            ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(dataSourceArr[i]))
                        )
                    }
                }
            }
            "[dash-mpd]" -> {
                // Create a data source factory.
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                val header = getDefaultRequestProperties(source)
                dataSourceFactory.setUserAgent(DEFAULT_USER_AGENT)
                dataSourceFactory.setDefaultRequestProperties(header)
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
    fun getDefaultRequestProperties(playerSource: BasePlayerSource): Map<String, String> {
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
    }
}