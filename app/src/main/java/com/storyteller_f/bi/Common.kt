package com.storyteller_f.bi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo2
import com.storyteller_f.bi.components.BangumiViewModel
import com.storyteller_f.bi.components.CommentId
import com.storyteller_f.bi.components.CommentReplyViewModel
import com.storyteller_f.bi.components.CommentViewModel
import com.storyteller_f.bi.components.FavoriteDetailViewModel
import com.storyteller_f.bi.components.FavoriteIdKey
import com.storyteller_f.bi.components.SeasonId
import com.storyteller_f.bi.components.UserBannerViewModel
import com.storyteller_f.bi.components.VideoId
import com.storyteller_f.bi.components.VideoIdLong
import com.storyteller_f.bi.components.VideoSearchViewModel
import com.storyteller_f.bi.components.VideoViewModel
import com.storyteller_f.bi.components.error
import com.storyteller_f.bi.components.loaded
import com.storyteller_f.bi.components.loading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class LoadingState {
    class Loading(val state: String) : LoadingState()
    class Error(val e: Throwable) : LoadingState()

    class Done(val itemCount: Int = 1) : LoadingState()
}

class LoadingHandler<T>(val handler: suspend () -> Unit) {
    val state: MutableLiveData<LoadingState> = MutableLiveData()
    val data: MutableLiveData<T> = MutableLiveData()
}

@Composable
fun OneCenter(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
fun StateView(state: LoadingState?, content: @Composable () -> Unit) =
    StateView(state = state, null) {
        content()
    }

@Composable
fun <Data> StateView(state: LoadingState?, data: Data?, content: @Composable (Data?) -> Unit) {
    when (state) {
        null -> OneCenter {
            Text(text = "waiting")
        }

        is LoadingState.Loading -> OneCenter {
            Text(text = "loading")
        }

        is LoadingState.Error -> OneCenter {
            Text(text = state.e.localizedMessage.orEmpty())
        }

        is LoadingState.Done -> if (state.itemCount == 0) OneCenter {
            Text(text = "empty")
        } else content(data)
    }
}

const val refreshAtLeastDelay = 300L

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StateView(handler: LoadingHandler<*>, content: @Composable () -> Unit) {
    val state by handler.state.observeAsState()
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = {
        refreshScope.launch {
            refreshing = true
            handler.handler
        }
    })
    LaunchedEffect(key1 = refreshing, key2 = state) {
        delay(refreshAtLeastDelay)
        if (refreshing && state !is LoadingState.Loading) refreshing = false
    }
    Box(modifier = Modifier.pullRefresh(refreshState)) {
        StateView(state, content)
        PullRefreshIndicator(refreshing, refreshState, Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> StateViewGeneric(handler: LoadingHandler<T>, content: @Composable (T?) -> Unit) {
    val state by handler.state.observeAsState()
    val observeAsState by handler.data.observeAsState()
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = {
        refreshScope.launch {
            refreshing = true
            handler.handler
        }
    })
    LaunchedEffect(key1 = refreshing, key2 = state) {
        delay(refreshAtLeastDelay)
        if (refreshing && state !is LoadingState.Loading) refreshing = false
    }
    Box(modifier = Modifier.pullRefresh(refreshState)) {
        StateView(state = state, data = observeAsState, content)
        PullRefreshIndicator(refreshing, refreshState, Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T : Any> StateView(pagingItems: LazyPagingItems<T>, function: @Composable () -> Unit) {
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = {
        refreshScope.launch {
            refreshing = true
            pagingItems.refresh()
        }
    })
    val refresh = pagingItems.loadState.refresh
    LaunchedEffect(key1 = refreshing, key2 = refresh) {
        delay(refreshAtLeastDelay)
        if (refreshing && refresh !is LoadState.Loading) refreshing = false
    }
    Box(modifier = Modifier.pullRefresh(refreshState)) {
        StateView(refresh, pagingItems.itemCount) {
            function()
        }
        PullRefreshIndicator(refreshing, refreshState, Modifier.align(Alignment.TopCenter))
    }
}


@Composable
fun StateView(state: LoadState?, count: Int = 1, content: @Composable () -> Unit) {
    val loadingState = when (state) {
        null -> null

        is LoadState.Loading -> LoadingState.Loading("loading")

        is LoadState.Error -> LoadingState.Error(state.error)

        is LoadState.NotLoading -> LoadingState.Done(count)
    }
    StateView(state = loadingState, content)
}

fun buildExtras(block: MutableCreationExtras.() -> Unit): MutableCreationExtras {
    return MutableCreationExtras().apply {
        block()
    }
}

val defaultFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val t = when (modelClass) {
            FavoriteDetailViewModel::class.java -> FavoriteDetailViewModel(extras[FavoriteIdKey]!!)
            VideoViewModel::class.java -> VideoViewModel(extras[VideoId]!!)
            CommentViewModel::class.java -> CommentViewModel(extras[VideoId]!!)
            CommentReplyViewModel::class.java -> CommentReplyViewModel(
                extras[VideoIdLong]!!,
                extras[CommentId]!!
            )

            VideoSearchViewModel::class.java -> VideoSearchViewModel()
            UserBannerViewModel::class.java -> UserBannerViewModel()
            BangumiViewModel::class.java -> BangumiViewModel(extras[VideoId]!!, extras[SeasonId]!!)
            else -> super.create(modelClass, extras)
        }
        return modelClass.cast(t)!!
    }
}

inline fun <T, R> request(
    state: MutableLiveData<LoadingState>,
    data: MutableLiveData<R>,
    service: () -> ResultInfo2<T>,
    build: (T) -> R
) {
    state.loading()
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
) = request(state, data, service) {
    it
}

inline fun <T> request(
    handler: LoadingHandler<T>,
    service: () -> ResultInfo2<T>,
) {
    val state = handler.state
    val data = handler.data
    request(state, data, service)
}