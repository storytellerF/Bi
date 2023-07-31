package com.storyteller_f.bi.components

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import bilibili.main.community.reply.v1.ReplyOuterClass
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerRepository
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
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.video.VideoSize
import com.storyteller_f.bi.Api
import com.storyteller_f.bi.LoadingHandler
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.defaultFactory
import com.storyteller_f.bi.unstable.PlayerDelegate
import com.storyteller_f.bi.unstable.VideoPlayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class PlayerKit(
    val size: VideoSize?,
    val mediaSource: MediaSource?,
    val progress: Long,
    val player: ExoPlayer,
    val reportHistory: () -> Unit
)

@Composable
fun rememberPlayerKit(
    videoPlayerRepository: BasePlayerRepository?,
    initProgress: Long
): State<PlayerKit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var size by remember {
        mutableStateOf<VideoSize?>(null)
    }
    var progress by remember {
        mutableLongStateOf(initProgress.coerceAtLeast(0L).seconds.inWholeMilliseconds)
    }

    val player = remember {
        Log.d("VideoPage", "VideoPage() called create player")
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    size = videoSize
                }
            })
        }
    }

    val reportProgress: () -> Unit = {
        scope.launch {
            videoPlayerRepository?.historyReport(player.currentPosition)
        }
    }
    DisposableEffect(key1 = player, effect = {
        Log.d("VideoPage", "VideoPage() called disposable")
        onDispose {
            Log.d("VideoPage", "VideoPage() called dispose invoked")
            progress = player.currentPosition
            reportProgress()
            player.stop()
            player.release()
        }
    })
    val mediaSource by produceState<MediaSource?>(
        initialValue = null,
        key1 = videoPlayerRepository
    ) {
        value = if (videoPlayerRepository != null)
            try {
                withContext(Dispatchers.IO) {
                    PlayerDelegate().mediaSource(context, videoPlayerRepository)
                }
            } catch (e: Exception) {
                null
            }
        else null
    }
    return remember {
        derivedStateOf {
            PlayerKit(size, mediaSource, progress, player, reportProgress)
        }
    }
}

@Composable
fun VideoPage(
    videoId: String = "",
    initProgress: Long,
    requestOrientation: ((Boolean) -> Unit)? = null
) {
    val videoViewModel =
        viewModel<VideoViewModel>(factory = defaultFactory, extras = MutableCreationExtras().apply {
            set(VideoId, videoId)
        })

    /**
     * 一般来说就是全屏的意思
     */
    var videoOnly by remember {
        mutableStateOf(false)
    }
    val uiControl = rememberSystemUiController()
    val videoInfo by videoViewModel.info.observeAsState()
    val videoPlayerRepository by videoViewModel.currentVideoRepository.observeAsState()
    val playerKit by rememberPlayerKit(
        videoPlayerRepository = videoPlayerRepository,
        initProgress = initProgress
    )
    val size = playerKit.size

    //全屏时依然保持竖屏状态
    val potentialPortrait = if (size != null) size.width < size.height else false
    val requestVideoOnly = if (requestOrientation != null) { it: Boolean ->
        videoOnly = it
        uiControl.isStatusBarVisible = !it
        uiControl.isNavigationBarVisible = !it
        if (!(potentialPortrait && it)) {
            requestOrientation.invoke(it)
        }
    } else null
    Log.d("VideoPage", "VideoPage() called")
    StateView(videoViewModel.handler) {
        Log.d("VideoPage", "VideoPage() called StateView")
        Column {
            if (!videoOnly)
                Text(text = videoId)
            VideoFrame(
                videoPlayerRepository,
                playerKit,
                !(potentialPortrait && videoOnly),//竖屏全屏时不用保持16:9的画面比例
                requestVideoOnly
            )
            if (!videoOnly) {
                val navController = rememberNavController()
                val navigate = { it: String ->
                    navController.navigate(it)
                }
                NavHost(navController = navController, startDestination = "description") {
                    videoPageNav(videoInfo, navigate, videoId)
                }
            }

        }
    }
}

/**
 * @param aspectRatio 是否保持播放器固定比例
 * @param requestVideoOnly 为null，说明不支持全屏
 */
@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun VideoFrame(
    videoPlayerRepository: BasePlayerRepository?,
    playerKit: PlayerKit,
    aspectRatio: Boolean,
    requestVideoOnly: ((Boolean) -> Unit)?
) {
    val cover = videoPlayerRepository?.coverUrl
    val mediaSource = playerKit.mediaSource
    if (mediaSource != null) {
        Log.d("VideoPage", "VideoPage() called VideoView ${playerKit.progress}")
        VideoView(
            playerKit.player,
            mediaSource,
            playerKit.progress,
            aspectRatio,
            playerKit.reportHistory,
            requestVideoOnly
        )
    } else {
        val coverModifier = Modifier.aspectRatio(16f / 9)
        StandBy(modifier = coverModifier) {
            GlideImage(
                model = cover?.let { UrlUtil.autoHttps(it) },
                contentDescription = "video cover",
                modifier = coverModifier
            )
        }
    }
}

@Preview
@Composable
private fun SubtitleTrack(
    checked: Boolean = true,
    text: String = "zh",
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text)
    }
}

private fun NavGraphBuilder.videoPageNav(
    videoInfo: VideoInfo?,
    navigate: (String) -> Unit,
    videoId: String
) {
    composable("description") {
        VideoDescription(videoInfo) {
            navigate("comments")
        }
    }
    composable("comments") {
        CommentsPage(videoId) {
            navigate("comment/${videoId}/$it")
        }
    }
    composable("comment/{vid}/{cid}", arguments = listOf(navArgument("vid") {
        type = NavType.LongType
    }, navArgument("cid") {
        type = NavType.LongType
    })) {
        val vid = it.arguments?.getLong("vid")!!
        val cid = it.arguments?.getLong("cid")!!
        CommentReplyPage(cid = cid, oid = vid)
    }
}

@Composable
private fun VideoView(
    player: ExoPlayer,
    mediaSource: MediaSource,
    seekTo: Long,
    aspectRatio: Boolean,
    videoStarted: () -> Unit,
    requestVideoOnly: ((Boolean) -> Unit)? = null,
) {
    AndroidView(
        factory = {
            StyledPlayerView(it)
        }, modifier = Modifier
            .fillMaxWidth()
            .let {
                if (aspectRatio) {
                    it.aspectRatio(16f / 9)
                } else it
            }
    ) {
        if (requestVideoOnly != null) {
            it.setFullscreenButtonClickListener { fullscreen ->
                requestVideoOnly(fullscreen)
            }
        }
        it.setShowSubtitleButton(true)
        it.player = player
        player.addMediaSource(mediaSource)
        player.prepare()
        player.seekTo(seekTo)
        player.play()
        videoStarted()
    }
}

class VideoInfoPreviewProvider : PreviewParameterProvider<VideoInfo> {
    override val values: Sequence<VideoInfo>
        get() = sequence {
            yield(
                VideoInfo(
                    "867109523",
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
    openComment: () -> Unit = {}
) {
    val pages = info?.pages
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
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
                    Column(modifier = Modifier
                        .apply {
                            if (it != 0) padding(start = 8.dp)
                        }
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(8.dp)) {
                        Text(text = pageDetail.page.toString())
                        Text(text = pageDetail.part, modifier = Modifier.widthIn(max = 200.dp))
                    }
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
        Button(onClick = { openComment() }) {
            Text(text = "open comments")
        }
    }

}

object VideoId : CreationExtras.Key<String>
object SeasonId : CreationExtras.Key<String>
object VideoIdLong : CreationExtras.Key<Long>
object CommentId : CreationExtras.Key<Long>

class VideoViewModel(private val videoId: String) : ViewModel() {
    val handler = LoadingHandler<VideoInfo>(::load)
    val state = handler.state
    val info = handler.data
    val currentVideoRepository = info.map { info ->
        VideoPlayerRepository(
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
            } catch (e: Throwable) {
                state.error(e)
            }
        }
    }
}

class CommentViewModel(oid: String) : ViewModel() {
    val flow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        CommentSource(oid)
    }.flow
        .cachedIn(viewModelScope)
}

class CommentSource(private val id: String) :
    PagingSource<ReplyOuterClass.CursorReply, ReplyOuterClass.ReplyInfo>() {
    override fun getRefreshKey(state: PagingState<ReplyOuterClass.CursorReply, ReplyOuterClass.ReplyInfo>): ReplyOuterClass.CursorReply? {
        return null
    }

    override suspend fun load(params: LoadParams<ReplyOuterClass.CursorReply>): LoadResult<ReplyOuterClass.CursorReply, ReplyOuterClass.ReplyInfo> {
        val key = params.key
        return try {
            val res = Api.requestCommentList(id, cursor = key)
            LoadResult.Page(
                res?.repliesList.orEmpty(),
                null,
                nextKey = res?.cursor?.takeIf { !it.isEnd })
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

}

class CommentReplyViewModel(oid: Long, commentId: Long) : ViewModel() {
    val flow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        CommentReplySource(oid, commentId)
    }.flow
        .cachedIn(viewModelScope)
}

class CommentReplySource(private val oid: Long, private val pid: Long) :
    PagingSource<ReplyOuterClass.CursorReply, ReplyOuterClass.ReplyInfo>() {
    override fun getRefreshKey(state: PagingState<ReplyOuterClass.CursorReply, ReplyOuterClass.ReplyInfo>): ReplyOuterClass.CursorReply? {
        return null
    }

    override suspend fun load(params: LoadParams<ReplyOuterClass.CursorReply>): LoadResult<ReplyOuterClass.CursorReply, ReplyOuterClass.ReplyInfo> {
        val key = params.key
        return try {
            val res = Api.requestCommentReply(oid, pid, key)
            LoadResult.Page(
                data = res?.root?.repliesList.orEmpty(),
                prevKey = null,
                nextKey = res?.cursor?.takeIf { !it.isEnd }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

}


@Composable
fun CommentsPage(videoId: String, viewComment: (Long) -> Unit = {}) {
    val data: CommentViewModel =
        viewModel(factory = defaultFactory, extras = MutableCreationExtras().apply {
            set(VideoId, videoId)
        })
    val pagingItems = data.flow.collectAsLazyPagingItems()
    CommentList(0, pagingItems, viewComment)
}

@Composable
fun CommentReplyPage(cid: Long, oid: Long) {
    val data: CommentReplyViewModel =
        viewModel(factory = defaultFactory, extras = MutableCreationExtras().apply {
            set(CommentId, cid)
            set(VideoIdLong, oid)
        })
    val pagingItems = data.flow.collectAsLazyPagingItems()
    CommentList(cid, pagingItems = pagingItems)
}

@Composable
private fun CommentList(
    parent: Long,
    pagingItems: LazyPagingItems<ReplyOuterClass.ReplyInfo>,
    viewComment: (Long) -> Unit = {}
) {
    StateView(pagingItems.loadState.refresh) {
        LazyColumn {
            topRefreshing(pagingItems)
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey(),
                contentType = pagingItems.itemContentType()
            ) { index ->
                val item = pagingItems[index]
                val info = item ?: ReplyOuterClass.ReplyInfo.getDefaultInstance()
                CommentItem(item = info, viewComment, parent)
            }
        }
    }
}

class CommentReplyListPreviewProvider : PreviewParameterProvider<List<ReplyOuterClass.ReplyInfo>> {
    override val values: Sequence<List<ReplyOuterClass.ReplyInfo>>
        get() = sequence {
            yield(buildList {
                add(buildUserComment("不知名用户1"))
                add(buildUserComment("不知名用户2", parent = 1L))
                add(buildUserComment("不知名用户3"))
            })
        }

}

@Preview
@Composable
private fun PreviewCommentReplyList(@PreviewParameter(CommentReplyListPreviewProvider::class) data: List<ReplyOuterClass.ReplyInfo>) {
    Column {
        data.forEach {
            CommentItem(item = it, parent = 0L)
        }
    }
}

class CommentItemPreviewProvider : PreviewParameterProvider<ReplyOuterClass.ReplyInfo> {
    override val values: Sequence<ReplyOuterClass.ReplyInfo>
        get() = sequence {
            yield(buildUserComment("不知名用户1"))
            yield(buildUserComment("mock 2"))
            yield(
                buildUserComment(
                    "you may not use this file except in compliance with the License. You may obtain a copy of the License at",
                )
            )
        }


}

private fun buildUserComment(userName: String, parent: Long = 0L) =
    ReplyOuterClass.ReplyInfo.newBuilder()
        .setMember(ReplyOuterClass.Member.newBuilder().setName(userName))
        .setContent(ReplyOuterClass.Content.newBuilder().setMessage("评论消息内容"))
        .setParent(parent)
        .setLike(100).build()

@Preview
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CommentItem(
    @PreviewParameter(CommentItemPreviewProvider::class) item: ReplyOuterClass.ReplyInfo,
    viewComment: (Long) -> Unit = {},
    parent: Long = 0,
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .padding(start = if (parent == item.parent) 0.dp else 24.dp)
            .fillMaxWidth()
            .clickable {
                viewComment(item.id)
            }
    ) {
        Row {
            val modifier = Modifier.size(30.dp)
            StandBy(modifier) {
                GlideImage(
                    model = UrlUtil.autoHttps(item.member.face),
                    contentDescription = "avatar",
                    modifier = modifier
                )
            }
            Text(
                text = item.member.name,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                maxLines = 2
            )
            Text(text = "like ${item.like} reply ${item.count}")
        }
        Text(text = item.content.message, modifier = Modifier.padding(top = 8.dp))
        Text(text = "${item.parent} ${item.dialog} ${item.id} ${item.type}")
    }
}