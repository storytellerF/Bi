package com.storyteller_f.bi.components

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a10miaomiao.bilimiao.comm.delegate.player.model.VideoPlayerSource
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.google.android.exoplayer2.source.MediaSource
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.unstable.PlayerDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Composable
fun VideoPage(videoId: String = "") {
    val videoViewModel = viewModel<VideoViewModel>(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return VideoViewModel(extras[VideoIdKey]!!) as T
        }
    }, extras = MutableCreationExtras().apply {
        set(VideoIdKey, videoId)
    })
    val current = LocalContext.current
    val info by videoViewModel.info.observeAsState()
    val rawState by videoViewModel.state.observeAsState()
    val pageData by videoViewModel.pagesData.observeAsState()
    val p = pageData
    LaunchedEffect(key1 = info) {
        val url = videoViewModel.getUrl(current)
        println(url)
    }
    when (val state = rawState) {
        is LoadingState.Loading -> Text(text = "loading")
        is LoadingState.Error -> Text(text = state.e.localizedMessage.orEmpty())
        is LoadingState.Done -> {
            Column {
                Text(text = videoId)
                Box(
                    modifier = Modifier
                        .aspectRatio(16f / 9)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Text(text = info?.title.orEmpty())
                val pubDate = info?.pubdate
                if (pubDate != null) {
                    DateUtils.formatDateTime(current, pubDate.seconds.inWholeMicroseconds, DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)
                    Text(text = pubDate.toString())
                }
                if (!p.isNullOrEmpty()) {
                    LazyRow {
                        items(p.size) {
                            val videoPageInfo = p[it]
                            Text(text = "${videoPageInfo.part}  - ${videoPageInfo.page}")
                        }
                    }
                }
                val tags = info?.tag.orEmpty()
                if (tags.isNotEmpty()) {
                    LazyRow {
                        items(tags.size) {
                            val tag = tags[it]
                            Text(
                                text = tag.tag_name,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }
                }
            }
        }

        else -> {
            Text(text = "impossible")
        }
    }
}

object VideoIdKey : CreationExtras.Key<String>

class VideoViewModel(private val videoId: String) : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    val info = MutableLiveData<VideoInfo>()
    val pagesData = info.map { data ->
        data.pages.map {
            it.copy(part = it.part.ifEmpty {
                data.title
            })
        }
    }
    val playData = info.map { info ->
        VideoPlayerSource(
            title = info.title,
            coverUrl = info.pic,
            aid = info.aid,
            id = info.cid.toString(),
            ownerId = info.owner.mid,
            ownerName = info.owner.name,
        )
    }

    suspend fun getUrl(context: Context): MediaSource? {
        val value = playData.value ?: return null
        return PlayerDelegate().mediaSource(context, value)
    }

    init {
        load()
    }

    private fun load() {
        state.loading()
        viewModelScope.launch {
            try {
                val type = if (videoId.indexOf("BV") == 0) {
                    "BV"
                } else {
                    "AV"
                }

                val res = withContext(Dispatchers.IO) {
                    BiliApiService.videoAPI
                        .info(videoId, type = type)
                        .call()
                        .gson<ResultInfo<VideoInfo>>()
                }

                if (res.code == 0) {
                    val data = res.data
                    info.value = data.copy(desc = BiliUrlMatcher.customString(data.desc))
                    state.loaded()
                } else {
                    state.error(res.message)
                }
            } catch (e: Exception) {
                state.error(e)
            }
        }
    }
}