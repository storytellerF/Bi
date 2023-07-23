package com.storyteller_f.bi.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import bilibili.app.interfaces.v1.HistoryOuterClass
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.Api
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.StateView
import kotlinx.coroutines.launch

fun MutableLiveData<LoadingState>.loaded() {
    value = LoadingState.Done()
}

fun MutableLiveData<LoadingState>.error(e: Throwable) {
    value = LoadingState.Error(e)
}

fun MutableLiveData<LoadingState>.error(e: String) {
    value = LoadingState.Error(Exception(e))
}

fun MutableLiveData<LoadingState>.loading(message: String = "") {
    value = LoadingState.Loading(message)
}

//todo 增加搜索功能
class HistoryViewModel : ViewModel() {
    val flow = Pager(
        PagingConfig(pageSize = 20)
    ) {
        HistoryPagingSource()
    }.flow
        .cachedIn(viewModelScope)

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HistoryPage(openVideo: (String, String, String, Long) -> Unit = { _, _, _, _ -> }) {
    val viewModel = viewModel<HistoryViewModel>()

    val lazyItems = viewModel.flow.collectAsLazyPagingItems()

    StateView(pagingItems = lazyItems) {
        LazyColumn {
            items(
                count = lazyItems.itemCount,
                key = lazyItems.itemKey {
                    it.oid.toString() + "" + it.kid.toString()
                },
                contentType = lazyItems.itemContentType()
            ) { index ->
                val item = lazyItems[index]
                val cursorItem = item ?: HistoryOuterClass.CursorItem.getDefaultInstance()
                HistoryItem(cursorItem, openVideo)
            }
            bottomAppending(lazyItems)
        }
    }
}

fun HistoryOuterClass.CursorItem.type(): String {
    return when {
        hasCardUgc() -> "ugc"
        hasCardOgv() -> "ogc"
        hasCardArticle() -> "article"
        hasCardLive() -> "live"
        else -> "cheese"
    }
}

fun HistoryOuterClass.CursorItem.progress(): Long {
    return when {
        hasCardUgc() -> cardUgc.progress
        hasCardOgv() -> cardOgv.progress
        hasCardArticle() -> 0L
        hasCardLive() -> 0L
        else -> cardCheese.progress
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
    openVideo: (String, String, String, Long) -> Unit
) {
    val text = item.title
    val progress = item.progress()
    val label = "${item.oid} ${item.kid} ${item.type()} ${item.business} $progress"
    VideoItem(item.cover(), text, label) {
        openVideo(item.kid.toString(), item.oid.toString(), item.business, progress)
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun VideoItem(
    pic: String? = null,
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
            .height((9 * 8).dp)
        StandBy(coverModifier) {
            val u = if (pic == null) null else "${UrlUtil.autoHttps(pic)}@672w_378h_1c_"
            GlideImage(u, contentDescription = null, modifier = coverModifier)
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = text, maxLines = 2)
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
            val res = Api.requestHistory(lastMax, lastTp)
            Log.i(TAG, "load: ${res?.cursor}")
            return LoadResult.Page(
                data = res?.itemsList.orEmpty(),
                prevKey = null, // Only paging forward.
                nextKey = res?.cursor?.takeIf { it.max != 0L }
            )
        } catch (e: Throwable) {
            // Handle errors in this block and return LoadResult.Error if it is an
            // expected error (such as a network failure).
            return LoadResult.Error(e)
        }
    }


    override fun getRefreshKey(state: PagingState<HistoryOuterClass.Cursor, HistoryOuterClass.CursorItem>): HistoryOuterClass.Cursor? {
        return null
    }

    companion object {
        private const val TAG = "HistoryPage"
    }
}