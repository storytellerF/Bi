package com.storyteller_f.bi.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(search: () -> Unit = {}, openDrawer: () -> Unit = {}) {
    val current = LocalContext.current
    TopAppBar(
        title = {
            Text(text = "Bi")
        },
        navigationIcon = {
            IconButton(onClick = {
                openDrawer()
            }) {
                Icon(Icons.Filled.Menu, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = {
                search()
            }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "search"
                )
            }
        }
    )
}