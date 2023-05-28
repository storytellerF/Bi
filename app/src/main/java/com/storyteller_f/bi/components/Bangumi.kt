package com.storyteller_f.bi.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo2
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiPublishInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiRatingInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiRightsInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiStatXInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.BangumiUserStatusInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.NewestEpisodeInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.SeasonSectionInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.buildExtras
import com.storyteller_f.bi.defaultFactory
import com.storyteller_f.bi.unstable.BangumiPlayerRepository
import kotlinx.coroutines.launch

class BangumiViewModel(val id: String, private val seasonId: String) : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    val info = MutableLiveData<BangumiInfo?>()

    val playState = MutableLiveData<LoadingState>()
    val list = MutableLiveData<SeasonSectionInfo?>()

    val current = MutableLiveData(id)
    val currentVideoRepository = current.switchMap { c ->
        info.map { bangumiInfo ->
            val v = bangumiInfo?.episodes?.firstOrNull {
                it.aid == c
            }
            if (v != null) {
                BangumiPlayerRepository(
                    sid = seasonId,
                    epid = v.ep_id,
                    aid = v.aid,
                    id = v.cid,
                    title = v.long_title.orEmpty().ifBlank { v.title ?: "-" },
                    coverUrl = v.cover,
                    ownerId = "",
                    ownerName = bangumiInfo.season_title
                )
            } else null
        }
    }

    init {
        refresh()
        loadEpisode()
    }

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
            request(state, info) {
                BiliApiService.bangumiAPI.seasonInfo(seasonId)
                    .awaitCall()
                    .gson()
            }
        }
    }

}

inline fun <T, R> request(
    state: MutableLiveData<LoadingState>,
    data: MutableLiveData<R>,
    service: () -> ResultInfo2<T>,
    build: (T) -> R
) {
    try {
        val res = service()
        val result = res.result
        if (res.isSuccess && result != null) {
            data.value = build(result)
            state.loaded()
        } else state.error(res.error())
    } catch (e: Exception) {
        state.error(e)
    }
}

inline fun <T> request(
    state: MutableLiveData<LoadingState>,
    data: MutableLiveData<T>,
    service: () -> ResultInfo2<T>,
) {
    state.loading()
    try {
        val res = service()
        val result = res.result
        if (res.isSuccess && result != null) {
            data.value = result
            state.loaded()
        } else state.error(res.error())
    } catch (e: Exception) {
        Log.e("request", "request: ", e)
        state.error(e)
    }
}

@Composable
fun BangumiPage(id: String, seasonId: String) {
    val bangumiViewModel =
        viewModel<BangumiViewModel>(factory = defaultFactory, extras = buildExtras {
            set(VideoId, id)
            set(SeasonId, seasonId)
        })
    val info by bangumiViewModel.info.observeAsState()
    val state by bangumiViewModel.state.observeAsState()
    val bangumiPlayerRepository by bangumiViewModel.currentVideoRepository.observeAsState()
    val playerKit by rememberPlayerKit(
        videoPlayerRepository = bangumiPlayerRepository,
        initProgress = 0
    )
    StateView(state = state) {
        Column {
            Text(text = "id $id season $seasonId")
            VideoFrame(
                playerKit,
                cover = info?.cover,
                aspectRatio = true,
                requestVideoOnly = null
            )
            BangumiDescription(info = info)
        }
    }
}

class BangumiPreviewProvider : PreviewParameterProvider<BangumiInfo?> {
    override val values: Sequence<BangumiInfo?>
        get() = sequence {
            yield(
                BangumiInfo(
                    "",
                    listOf(),
                    "",
                    0,
                    "",
                    0,
                    0,
                    0,
                    NewestEpisodeInfo("", 0, "", 0, ""),
                    BangumiPublishInfo(),
                    BangumiRatingInfo(4339, 9.6),
                    "",
                    BangumiRightsInfo(
                        0, 0,
                        1,
                        1,
                        0,
                        1,
                        1,
                        0,
                        1,
                        1
                    ),
                    0,
                    0,
                    "",
                    0,
                    listOf(),
                    0,
                    "",
                    "",
                    BangumiStatXInfo("277922", "105087", "480319", "42630", "30029", "57644979"),
                    "英雄之路",
                    0,
                    BangumiUserStatusInfo(0, 0, 0, 0, 0, BangumiUserStatusInfo.WatchProgressInfo(0, "", 0))
                )
            )
        }

}

@Preview
@Composable
fun BangumiDescription(@PreviewParameter(BangumiPreviewProvider::class) info: BangumiInfo?) {
    if (info != null) {
        Text(text = info.title)
    }
}