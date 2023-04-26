package com.storyteller_f.bi.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.storyteller_f.bi.R

val bottomNavigationItems = listOf(
    Screen.History,
    Screen.Moments,
    Screen.Playlist,
    Screen.Favorite
)

@Preview
@Composable
fun HomeNavigation(
    currentRoute: String? = Screen.Favorite.route,
    selectRoute: (Screen) -> Unit = {}
) {
    NavigationBar {
        bottomNavigationItems.forEach { screen ->
            NavigationBarItem(selected = currentRoute == screen.route, onClick = {
                selectRoute(screen)
            }, {
                when {
                    screen.icon != null -> {
                        Icon(
                            ImageVector.vectorResource(id = screen.icon),
                            contentDescription = screen.route
                        )
                    }

                    screen.vector != null -> Icon(
                        screen.vector,
                        contentDescription = screen.route
                    )
                }
            }, label = {
                Text(text = stringResource(id = screen.resourceId))
            })
        }
        NavigationBarItem(selected = false, onClick = {

        }, {
            Icon(
                ImageVector.vectorResource(id = R.drawable.baseline_history_24),
                contentDescription = null
            )
        }, label = {
            Text(text = "special")
        })
    }
}