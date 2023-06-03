package com.storyteller_f.bi

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

sealed class LoadingState {
    class Loading(val state: String) : LoadingState()
    class Error(val e: Throwable) : LoadingState()

    class Done(val itemCount: Int = 1) : LoadingState()
}

@Composable
fun OneCenter(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
fun StateView(state: LoadingState?, content: @Composable () -> Unit) {
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
        } else content()
    }
}

@Composable
fun <T : Any> StateView(pagingItems: LazyPagingItems<T>, function: @Composable () -> Unit) {
    StateView(pagingItems.loadState.refresh, pagingItems.itemCount) {
        function()
    }
}


@Composable
fun StateView(state: LoadState?, count: Int = 1, content: @Composable () -> Unit) {
    when (state) {
        null -> OneCenter {
            Text(text = "waiting")
        }

        is LoadState.Loading -> OneCenter {
            Text(text = "loading")
        }

        is LoadState.Error -> OneCenter {
            Text(text = state.error.localizedMessage.orEmpty())
        }

        is LoadState.NotLoading -> if (count == 0) OneCenter {
            Text(text = "empty")
        } else content()
    }
}

@Composable
fun ErrorStateView(state: LoadState?, content: @Composable () -> Unit) {
    when (state) {
        null -> OneCenter {
            Text(text = "waiting")
        }

        is LoadState.Error -> OneCenter {
            Text(text = state.error.localizedMessage.orEmpty())
        }

        else -> content()
    }
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