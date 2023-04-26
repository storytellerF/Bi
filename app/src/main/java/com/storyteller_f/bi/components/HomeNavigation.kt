package com.storyteller_f.bi.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun HomeNavigation(
    currentRoute: String? = Screen.Favorite.route,
    selectRoute: (Screen) -> Unit = {}
) {
    val density = LocalDensity.current
    AnimatedVisibility(visible = currentRoute != null, enter = slideInVertically {
        with(density) { 40.dp.roundToPx() }
    }, exit = slideOutVertically()) {
        NavigationBar {
            Screen.bottomNavigationItems.forEach { screen ->
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
        }
    }
}