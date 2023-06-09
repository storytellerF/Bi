package com.storyteller_f.bi.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.storyteller_f.bi.R

sealed class Screen(
    val route: String,
    @StringRes val resourceId: Int,
    val vector: ImageVector? = null,
    @DrawableRes val icon: Int? = null
) {
    object History : Screen("histories", R.string.histories, icon = R.drawable.baseline_history_24)
    object Moments : Screen("moments", R.string.moments, icon = R.drawable.baseline_explore_24)
    object Playlist : Screen("playlist", R.string.playlist, vector = Icons.Filled.PlayArrow)
    object Favorite : Screen("favorites", R.string.favorite, vector = Icons.Filled.Favorite)

    object Search : Screen("search", R.string.search, vector = Icons.Filled.Search)

    object FavoriteList : Screen("favorite-detail/{id}", R.string.favorite, vector = Icons.Filled.Favorite)
    companion object {
        val allRoute =
            listOf(History, Moments, Playlist, Favorite, Search, FavoriteList)

        val bottomNavigationItems = listOf(
            History,
            Moments,
            Playlist,
            Favorite,
        )
    }
}

