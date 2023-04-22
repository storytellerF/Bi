package com.storyteller_f.bi.components

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerSource
import com.a10miaomiao.bilimiao.comm.delegate.player.model.VideoPlayerSource
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoOwnerInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoPageInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoReqUserInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoStatInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoTagInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem.fromUri
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.unstable.PlayerDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun VideoPage(videoId: String = "") {
    val videoViewModel = viewModel<VideoViewModel>(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(extras[VideoIdKey]!!) as T
        }
    }, extras = MutableCreationExtras().apply {
        set(VideoIdKey, videoId)
    })
    val current = LocalContext.current
    val scope = rememberCoroutineScope()
    val info by videoViewModel.info.observeAsState()
    val playerSourceState by videoViewModel.playerSource.observeAsState()
    val rawState by videoViewModel.state.observeAsState()
    var mediaSourceState by remember {
        mutableStateOf<MediaSource?>(null)
    }

    val playerSource = playerSourceState
    val mediaSource = mediaSourceState
    val player = remember {
        ExoPlayer.Builder(current).build()
    }
    DisposableEffect(key1 = player, effect = {
        onDispose {
            scope.launch {
                playerSourceState?.historyReport(player.currentPosition)
            }
            player.stop()
            player.release()
        }
    })
    LaunchedEffect(key1 = playerSourceState) {
        if (playerSource != null) {
            val sourceInfo = withContext(Dispatchers.IO) {
                current.sourcePair(playerSource)
            }
            mediaSourceState = sourceInfo
        }
    }
    StateView(state = rawState) {
        Column {
            Text(text = videoId)
            if (mediaSource != null) {
                AndroidView(
                    factory = {
                        StyledPlayerView(it)
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9)
                ) {
                    it.player = player
                    player.addMediaSource(mediaSource)
                    player.prepare()
                    player.play()
                    scope.launch {
                        playerSource?.historyReport(player.currentPosition)
                    }
                }

            }

            VideoDescription(info)
        }
    }
}

class VideoInfoPreviewProvider : PreviewParameterProvider<VideoInfo> {
    override val values: Sequence<VideoInfo>
        get() = sequence {
            yield(
                VideoInfo(
                    "",
                    1,
                    "",
                    0L,
                    1,
                    0.0,
                    "",
                    1,
                    1,
                    "",
                    VideoOwnerInfo("", "", ""),
                    listOf(VideoPageInfo("", "", 1, "", 1, "part", "", "")),
                    "",
                    1681045208L,
                    null,
                    VideoReqUserInfo(1, 1, 1, 1, 1),
                    null,
                    null,
                    VideoStatInfo("", ""),
                    1,
                    listOf(
                        VideoTagInfo(1, "", 1, 1, 1, 1, 1, 0.0, "tag name"),
                        VideoTagInfo(1, "", 1, 1, 1, 1, 1, 0.1, "tag name")
                    ),
                    1,
                    "video title",
                    "",
                    1,
                    null
                )
            )
        }

}

@Preview
@Composable
private fun VideoDescription(
    @PreviewParameter(VideoInfoPreviewProvider::class) info: VideoInfo?,
) {
    val pages = info?.pages
    Column(modifier = Modifier.padding(8.dp)) {
        val current = LocalContext.current
        Text(text = info?.title.orEmpty())
        val pubDate = info?.pubdate
        if (pubDate != null) {
            val formatDateTime = DateUtils.formatDateTime(
                current,
                pubDate * 1000,
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
            )
            Text(text = formatDateTime)
        }
        if (!pages.isNullOrEmpty()) {
            LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                items(pages.size) {
                    val pageDetail = pages[it]
                    Text(
                        text = "${pageDetail.part}  - ${pageDetail.page}",
                        modifier = Modifier
                            .apply {
                                if (it != 0) padding(start = 8.dp)
                            }
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(8.dp))
                }
            }
        }
        val tags = info?.tag.orEmpty()
        if (tags.isNotEmpty()) {
            LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                items(tags, {
                    it.tag_id
                }) {
                    Text(
                        text = it.tag_name,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(4.dp)

                    )
                }
            }
        }
    }

}

object VideoIdKey : CreationExtras.Key<String>

class VideoViewModel(private val videoId: String) : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    val info = MutableLiveData<VideoInfo>()
    val playerSource = info.map { info ->
        VideoPlayerSource(
            title = info.title,
            coverUrl = info.pic,
            aid = info.aid,
            id = info.cid.toString(),
            ownerId = info.owner.mid,
            ownerName = info.owner.name,
        )
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
                    info.value = data.copy(
                        desc = BiliUrlMatcher.customString(data.desc),
                        pages = data.pages.map {
                            it.copy(part = it.part.ifEmpty {
                                data.title
                            })
                        })
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

suspend fun Context.sourcePair(playerSource: BasePlayerSource): MediaSource {
    val sourceInfo = playerSource.getPlayerUrl(64, 1)

    val url = sourceInfo.url
    return when {
        url.contains("\n") -> PlayerDelegate().mediaSource(this, playerSource)
        else -> {
            val factory = DefaultHttpDataSource.Factory()
            factory.setUserAgent(PlayerDelegate.DEFAULT_USER_AGENT)
            factory.setDefaultRequestProperties(
                PlayerDelegate().getDefaultRequestProperties(
                    playerSource
                )
            )
            ProgressiveMediaSource.Factory(factory).createMediaSource(fromUri(url))
        }
    }
}