package com.storyteller_f.bi.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import com.storyteller_f.bi.R

sealed class Screen(val route: String, @StringRes val resourceId: Int, val vector: ImageVector? = null, @DrawableRes val icon: Int? = null) {
    object History : Screen("history", R.string.history, icon = R.drawable.baseline_history_24)
    object Moments : Screen("moments", R.string.moments, vector = Icons.Filled.Menu)
}