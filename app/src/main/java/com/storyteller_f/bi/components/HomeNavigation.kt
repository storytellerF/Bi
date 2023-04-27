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

@Preview
@Composable
fun HomeNavigation(
    currentRoute: String? = Screen.Favorite.route,
    selectRoute: (String) -> Unit = {}
) {
    NavigationBar {
        Screen.bottomNavigationItems.forEach { screen ->
            NavigationBarItem(selected = currentRoute == screen.route, onClick = {
                selectRoute(screen.route)
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