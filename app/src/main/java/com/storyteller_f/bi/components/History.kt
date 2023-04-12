package com.storyteller_f.bi.components

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import bilibili.app.interfaces.v1.HistoryGrpc
import bilibili.app.interfaces.v1.HistoryOuterClass
import com.a10miaomiao.bilimiao.comm.network.request
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.ErrorStateView
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.OneCenter
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.VideoActivity

fun MutableLiveData<LoadingState>.loaded() {
    value = LoadingState.Done
}

fun MutableLiveData<LoadingState>.error(e: Exception) {
    value = LoadingState.Error(e)
}

fun MutableLiveData<LoadingState>.error(e: String) {
    value = LoadingState.Error(Exception(e))
}

fun MutableLiveData<LoadingState>.loading(message: String = "") {
    value = LoadingState.Loading(message)
}

class HistoryViewModel : ViewModel() {
    val flow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        HistoryPagingSource()
    }.flow
        .cachedIn(viewModelScope)

}

@Composable
fun HistoryPage() {
    val viewModel = viewModel<HistoryViewModel>()
    val lazyItems = viewModel.flow.collectAsLazyPagingItems()
    StateView(state = lazyItems.loadState.refresh) {
        LazyColumn {
            items(lazyItems, {
                it.oid.toString() + "" + it.kid.toString()
            }) { item ->
                HistoryItem(item ?: HistoryOuterClass.CursorItem.getDefaultInstance())
            }
            bottomAppending(lazyItems)
        }
    }
}

class VideoItemProvider : PreviewParameterProvider<HistoryOuterClass.CursorItem> {
    override val values: Sequence<HistoryOuterClass.CursorItem>
        get() = sequence {
            yield(HistoryOuterClass.CursorItem.getDefaultInstance())
            yield(HistoryOuterClass.CursorItem.getDefaultInstance())
        }

}

@Composable
fun HistoryItem(
    @PreviewParameter(VideoItemProvider::class) item: HistoryOuterClass.CursorItem,
) {
    val current = LocalContext.current
    val text = item.title
    val label = item.dt.type.name
    VideoItem(item.cover(), text, label) {
        current.startActivity(Intent(current, VideoActivity::class.java).apply {
            putExtra("videoId", item.oid.toString())
        })
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun VideoItem(
    pic: String = "",
    text: String = "text",
    label: String = "label",
    watchVideo: () -> Unit = {}
) {
    Row(modifier = Modifier
        .padding(8.dp)
        .clickable {
            watchVideo()
        }) {
        val coverModifier = Modifier
            .width((16 * 8).dp)
            .height((8 * 8).dp)
        StandBy(width = 16 * 8, height = 8 * 8) {
            val u = "${UrlUtil.autoHttps(pic)}@672w_378h_1c_"
            GlideImage(u, contentDescription = null, modifier = coverModifier)
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = text)
            Text(text = label)
        }
    }
}

fun HistoryOuterClass.CursorItem.cover(): String {
    return if (hasCardOgv()) {
        cardOgv.cover
    } else {
        cardUgc.cover
    }
}

class HistoryPagingSource : PagingSource<HistoryOuterClass.Cursor, HistoryOuterClass.CursorItem>() {
    override suspend fun load(
        params: LoadParams<HistoryOuterClass.Cursor>
    ): LoadResult<HistoryOuterClass.Cursor, HistoryOuterClass.CursorItem> {
        try {
            /**
             * 历史结果
             * load: 0 0
             * load: 1680969219 3
             * load: 1680968482 3
             * load: 1680963212 3
             */
            val (lastMax, lastTp) = if (params.key != null) {
                (params.key?.max ?: 0L) to (params.key?.maxTp ?: 0)
            } else 0L to 0
            Log.i(TAG, "load: $lastMax $lastTp")
            val req = HistoryOuterClass.CursorV2Req.newBuilder().apply {
                business = "archive"
                cursor = HistoryOuterClass.Cursor.newBuilder().apply {
                    if (lastMax != 0L) {
                        max = lastMax
                        maxTp = lastTp
                    }
                }.build()
            }.build()
            val res = HistoryGrpc.getCursorV2Method()
                .request(req)
                .awaitCall()
            return LoadResult.Page(
                data = res.itemsList,
                prevKey = null, // Only paging forward.
                nextKey = res.cursor
            )
        } catch (e: Exception) {
            // Handle errors in this block and return LoadResult.Error if it is an
            // expected error (such as a network failure).
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<HistoryOuterClass.Cursor, HistoryOuterClass.CursorItem>): HistoryOuterClass.Cursor? {
        return null
    }

    companion object {
        private const val TAG = "History"
    }
}