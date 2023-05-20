package com.storyteller_f.bi.components

import android.content.Context
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
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerSource
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
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.unstable.PlayerDelegate
import com.storyteller_f.bi.unstable.VideoPlayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Composable
fun VideoPage(
    videoId: String = "",
    initProgress: Long,
    requestOrientation: ((Boolean) -> Unit)? = null
) {
    val videoViewModel =
        viewModel<VideoViewModel>(factory = defaultFactory, extras = MutableCreationExtras().apply {
            set(VideoIdKey, videoId)
        })

    /**
     * 一般来说就是全屏的意思
     */
    var videoOnly by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiControl = rememberSystemUiController()
    val videoInfo by videoViewModel.info.observeAsState()
    val videoPlayerRepository by videoViewModel.playerSource.observeAsState()
    val loadingState by videoViewModel.state.observeAsState()
    var mediaSource by remember {
        mutableStateOf<MediaSource?>(null)
    }
    var size by remember {
        mutableStateOf<VideoSize?>(null)
    }
    var progress by remember {
        mutableStateOf(initProgress.coerceAtLeast(0L).seconds.inWholeMilliseconds)
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
    LaunchedEffect(key1 = videoPlayerRepository) {
        Log.d("VideoPage", "VideoPage() called try get source $videoPlayerRepository")
        videoPlayerRepository?.let {
            mediaSource = withContext(Dispatchers.IO) {
                context.sourcePair(it)
            }
        }

    }
    Log.d("VideoPage", "VideoPage() called")
    StateView(state = loadingState) {
        Log.d("VideoPage", "VideoPage() called StateView")
        val playerMediaSource = mediaSource
        val s = size

        val potentialPortrait = if (s != null) s.width < s.height else false
        val requestVideoOnly = if (requestOrientation != null) { it: Boolean ->
            progress = player.currentPosition
            videoOnly = it
            uiControl.isStatusBarVisible = !it
            uiControl.isNavigationBarVisible = !it
            if (!(potentialPortrait && it)) {
                requestOrientation.invoke(it)
            }
        } else null
        Column {
            if (!videoOnly)
                Text(text = videoId)
            if (playerMediaSource != null) {
                Log.d("VideoPage", "VideoPage() called VideoView $progress")

                VideoView(
                    player,
                    playerMediaSource,
                    progress,
                    !(potentialPortrait && videoOnly),
                    reportProgress,
                    requestVideoOnly
                )
            }
            if (!videoOnly) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "description") {
                    composable("description") {
                        VideoDescription(videoInfo) {
                            navController.navigate("comments")
                        }
                    }
                    composable("comments") {
                        CommentsPage(videoId) {
                            navController.navigate("comment/${videoId}/$it")
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
            }

        }
    }
}

@Composable
private fun VideoView(
    player: ExoPlayer,
    mediaSource: MediaSource,
    seekTo: Long,
    aspectRatio: Boolean,
    reportProgress: () -> Unit,
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
        reportProgress()
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

object VideoIdKey : CreationExtras.Key<String>
object VideoIdLongKey : CreationExtras.Key<Long>
object CommentIdKey : CreationExtras.Key<Long>

class VideoViewModel(private val videoId: String) : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    val info = MutableLiveData<VideoInfo>()
    val playerSource = info.map { info ->
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

suspend fun Context.sourcePair(playerSource: BasePlayerSource): MediaSource {
    return PlayerDelegate().mediaSource(this, playerSource)
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
            set(VideoIdKey, videoId)
        })
    val pagingItems = data.flow.collectAsLazyPagingItems()
    CommentList(0, pagingItems, viewComment)
}

@Composable
fun CommentReplyPage(cid: Long, oid: Long) {
    val data: CommentReplyViewModel =
        viewModel(factory = defaultFactory, extras = MutableCreationExtras().apply {
            set(CommentIdKey, cid)
            set(VideoIdLongKey, oid)
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