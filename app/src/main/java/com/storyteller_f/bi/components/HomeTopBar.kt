package com.storyteller_f.bi.components

import android.content.Intent
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
import com.storyteller_f.bi.SearchActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(openDrawer: () -> Unit) {
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
                current.startActivity(Intent(current, SearchActivity::class.java))
            }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "search"
                )
            }
        }
    )
}