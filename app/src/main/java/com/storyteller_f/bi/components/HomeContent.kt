package com.storyteller_f.bi.components

import androidx.annotation.StringRes
import com.storyteller_f.bi.R

sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object History : Screen("history", R.string.history)
    object Moments : Screen("moments", R.string.moments)
}