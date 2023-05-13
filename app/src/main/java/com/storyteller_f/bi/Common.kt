package com.storyteller_f.bi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.a10miaomiao.bilimiao.comm.entity.search.SearchVideoInfo

sealed class LoadingState {
    class Loading(val state: String) : LoadingState()
    class Error(val e: Throwable) : LoadingState()

    object Done : LoadingState()
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
        is LoadingState.Done -> content()
    }
}

@Composable
fun StateView(collectAsLazyPagingItems: LazyPagingItems<SearchVideoInfo>, function: @Composable () -> Unit) {
    StateView(collectAsLazyPagingItems.loadState.refresh) {
        function()
    }
}


@Composable
fun StateView(state: LoadState?, content: @Composable () -> Unit) {
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
        is LoadState.NotLoading -> content()
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