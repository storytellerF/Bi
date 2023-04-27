package com.storyteller_f.bi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState

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
