package com.storyteller_f.bi

sealed class LoadingState {
    class Loading(val state: String) : LoadingState()
    class Error(val e: Exception) : LoadingState()

    object Done : LoadingState()
}