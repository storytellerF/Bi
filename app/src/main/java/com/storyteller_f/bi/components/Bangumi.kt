package com.storyteller_f.bi.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiPublishInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiRatingInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiRightsInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiStatXInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiUserStatusInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.EpisodeInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.NewestEpisodeInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.SeasonSectionInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.storyteller_f.bi.LoadingHandler
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.buildExtras
import com.storyteller_f.bi.defaultFactory
import com.storyteller_f.bi.request
import com.storyteller_f.bi.unstable.BangumiPlayerRepository
import kotlinx.coroutines.launch

class BangumiViewModel(val id: String, private val seasonId: String) : ViewModel() {
    val bangumiHandler = LoadingHandler<BangumiInfo?>(::refresh)

    private val playState = MutableLiveData<LoadingState>()
    private val list = MutableLiveData<SeasonSectionInfo?>()

    private val current = MutableLiveData(id)
    val currentVideoRepository = current.switchMap { c ->
        bangumiHandler.data.map { bangumiInfo ->
            val v = bangumiInfo?.episodes?.firstOrNull {
                it.aid == c
            }
            if (v != null) {
                BangumiPlayerRepository(
                    sid = seasonId,
                    epid = v.epId,
                    aid = v.aid,
                    id = v.cid,
                    title = v.safeTitle,
                    coverUrl = v.cover,
                    ownerId = "",
                    ownerName = bangumiInfo.season_title
                )
            } else null
        }
    }

    init {
        refresh()
//        loadEpisode()
    }

    @Suppress("unused")
    private fun loadEpisode() {
        viewModelScope.launch {
            request(playState, list) {
                BiliApiService.bangumiAPI.seasonSection(seasonId)
                    .awaitCall()
                    .gson()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            request(bangumiHandler) {
                BiliApiService.bangumiAPI.seasonInfo(seasonId)
                    .awaitCall()
                    .gson()
            }
        }
    }

}

@Composable
fun BangumiPage(id: String, seasonId: String) {
    val bangumiViewModel =
        viewModel<BangumiViewModel>(factory = defaultFactory, extras = buildExtras {
            set(VideoId, id)
            set(SeasonId, seasonId)
        })
    val info by bangumiViewModel.bangumiHandler.data.observeAsState()
    val state by bangumiViewModel.bangumiHandler.state.observeAsState()
    val bangumiPlayerRepository by bangumiViewModel.currentVideoRepository.observeAsState()
    val playerKit by rememberPlayerKit(
        videoPlayerRepository = bangumiPlayerRepository,
        initProgress = 0
    )
    StateView(bangumiViewModel.bangumiHandler) {
        Column {
            Text(text = "id $id season $seasonId")
            VideoFrame(
                bangumiPlayerRepository,
                playerKit,
                aspectRatio = true,
                requestVideoOnly = null
            )
            BangumiDescription(info = info)
        }
    }
}

@Preview
@Composable
fun BangumiDescription(@PreviewParameter(BangumiPreviewProvider::class) info: BangumiInfo?) {
    Surface {
        if (info != null) {
            Column {
                Text(text = info.title)
                val main = info.episodes.take(info.total_ep)
                val other = info.episodes.safeSub(info.total_ep, info.episodes.size)
                val episodeModifier =
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp).sizeIn(maxWidth = 80.dp)
                val color = MaterialTheme.colorScheme.onPrimaryContainer
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(main.size) {
                        Text(text = main[it].safeTitle, color = color, modifier = episodeModifier)
                    }
                }
                LazyRow {
                    items(other.size) {
                        Text(text = other[it].safeTitle,color = color, modifier = episodeModifier)
                    }
                }
            }
        }
    }
}

fun <T> List<T>.safeSub(startIndex: Int, endIndex: Int): List<T> {
    if (startIndex >= size) return emptyList()
    if (endIndex <= startIndex) return emptyList()
    return subList(startIndex, endIndex)
}

class BangumiPreviewProvider : PreviewParameterProvider<BangumiInfo?> {
    override val values: Sequence<BangumiInfo?>
        get() = sequence {
            yield(
                BangumiInfo(
                    "http://i0.hdslb.com/bfs/bangumi/image/e35c752ae6938f22fbf9e3743bdacf1c8b7d49bd.jpg",
                    listOf(
                        EpisodeInfo(
                            aid = "256833820",
                            attr = 144,
                            badgeText = "会员",
                            badgeType = 0,
                            bvid = "BV1zY411F7YU",
                            cid = "744358767",
                            cover = "http://i0.hdslb.com/bfs/archive/4e2da981dfb296c114837b591dea8bd027aebfc0.jpg",
                            ctime = "2022-05-19T17:05:19",
                            duration = 1715000,
                            epId = "511805",
                            episodeStatus = 13,
                            episodeType = 0,
                            from = "bangumi",
                            index = "1",
                            indexTitle = "司马迁 往事不成空",
                            mid = 7584632,
                            page = 1,
                            premiere = false,
                            pubRealTime = "2022-05-23T18:00:00",
                            sectionId = 62876,
                            sectionType = 0,
                            shareUrl = "https://m.bilibili.com/bangumi/play/ep511805",
                            vid = ""
                        ),
                        EpisodeInfo(
                            aid = "214390336",
                            attr = 144,
                            badgeText = "会员",
                            badgeType = 0,
                            bvid = "BV1ba411E74T",
                            cid = "738617516",
                            cover = "http://i0.hdslb.com/bfs/archive/238b3f20db852eff95db7a9a475a8d3c59110b0b.jpg",
                            ctime = "2022-05-19T17:06:03",
                            duration = 1683000,
                            epId = "511806",
                            episodeStatus = 13,
                            episodeType = 0,
                            from = "bangumi",
                            index = "2",
                            indexTitle = "耿恭 天山下的勇士",
                            mid = 7584632,
                            page = 1,
                            premiere = false,
                            pubRealTime = "2022-05-30T18:00:00",
                            sectionId = 62876,
                            sectionType = 0,
                            shareUrl = "https://m.bilibili.com/bangumi/play/ep511806",
                            vid = ""
                        )
                    ),
                    "请升级至新版APP付费观看",
                    1,
                    "http://www.bilibili.com/bangumi/media/md28235565/",
                    28235565,
                    0,
                    2,
                    NewestEpisodeInfo("已完结, 全6集", 511810, "6", 0, "1656324000000"),
                    BangumiPublishInfo(),
                    BangumiRatingInfo(4339, 9.6),
                    "",
                    BangumiRightsInfo(
                        0,
                        0,
                        1,
                        1,
                        0,
                        1,
                        1,
                        0,
                        1,
                        1
                    ),
                    39871,
                    13,
                    "",
                    3,
                    listOf(),
                    0,
                    "http://m.bilibili.com/bangumi/play/ss39871",
                    "http://i0.hdslb.com/bfs/bangumi/image/bbb28e4bf4d83f7c8268b7cc976c6cf44c35c94e.jpg",
                    BangumiStatXInfo("277922", "105087", "480319", "42630", "30029", "57644979"),
                    "英雄之路",
                    6,
                    BangumiUserStatusInfo(
                        0,
                        0,
                        0,
                        0,
                        0,
                        BangumiUserStatusInfo.WatchProgressInfo(0, "", 0)
                    )
                )
            )
        }

}

