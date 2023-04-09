package com.a10miaomiao.bilimiao.comm.delegate.player.model

import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerSource
import com.a10miaomiao.bilimiao.comm.delegate.player.entity.PlayerSourceInfo
import com.a10miaomiao.bilimiao.comm.delegate.player.entity.SubtitleSourceInfo
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.player.PlayerV2Info
import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.CompressionTools
import java.io.ByteArrayInputStream
import java.io.InputStream

class VideoPlayerSource(
    override val title: String,
    override val coverUrl: String,
    var aid: String, // av号
    override var id: String, // cid
    override val ownerId: String,
    override val ownerName: String,
): BasePlayerSource() {

    override suspend fun getPlayerUrl(quality: Int, fnval: Int): PlayerSourceInfo {
        val res = BiliApiService.playerAPI
            .getVideoPalyUrl(aid, id, quality, fnval)
        val dash = res.dash
        var duration: Long
        val url = if (dash != null) {
            duration = dash.duration * 1000L
            DashSource(res.quality, dash).getMDPUrl()
        } else {
            val durl = res.durl!!
            if (durl.size == 1) {
                duration = durl[0].length * 1000L
                durl[0].url
            } else {
                duration = 0L
                "[concatenating]\n" + durl.joinToString("\n") {
                    duration += it.length * 1000L
                    it.url
                }
            }
        }
        val acceptDescription = res.accept_description
        val acceptList = res.accept_quality.mapIndexed { index, i ->
            PlayerSourceInfo.AcceptInfo(i, acceptDescription[index])
        }
        return PlayerSourceInfo(url, res.quality, acceptList, duration)
    }

    override suspend fun getSubtitles(): List<SubtitleSourceInfo> {
        try {
            val res = BiliApiService.playerAPI
                .getPlayerV2Info(aid = aid, cid = id)
                .awaitCall()
                .gson<ResultInfo<PlayerV2Info>>()
            if (res.isSuccess) {
                return res.data.subtitle.subtitles.map {
                    SubtitleSourceInfo(
                        id = it.id,
                        lan = it.lan,
                        lan_doc = it.lan_doc,
                        subtitle_url = it.subtitle_url,
                        ai_status = it.ai_status,
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    override suspend fun historyReport(progress: Long) {
        try {
            val realtimeProgress = progress.toString()  // 秒数
            MiaoHttp.request {
                url = "https://api.bilibili.com/x/v2/history/report"
                formBody = ApiHelper.createParams(
                    "aid" to aid,
                    "cid" to id,
                    "progress" to realtimeProgress,
                    "realtime" to realtimeProgress,
                    "type" to "3"
                )
                method = MiaoHttp.POST
            }.awaitCall()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}