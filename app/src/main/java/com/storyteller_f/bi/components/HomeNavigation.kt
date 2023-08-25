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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.storyteller_f.bi.UserAware

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
                NavItemIcon(screen)
            }, label = {
                Text(text = stringResource(id = screen.resourceId))
            })
        }
    }
}

@Composable
fun NavItemIcon(screen: Screen) {
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
}

/**
 * @param openVideo 参数解释 id，id， business，progress。如果当前是番剧，第一个是kid， 第二个是oid，oid 就是episode 中视频的aid， kid 就是session id。
 */
fun NavGraphBuilder.homeNav(
    selectRoute: (String) -> Unit,
    login: () -> Unit = {},
    openVideo: (String, String, String, Long) -> Unit
) {
    composable(Screen.History.route) {
        UserAware(login) {
            HistoryPage(openVideo)
        }
    }
    composable(Screen.Moments.route) {
        UserAware(login) {
            MomentsPage(openVideo)
        }
    }
    composable(Screen.Playlist.route) {
        UserAware(login) {
            PlaylistPage(openVideo)
        }
    }
    composable(Screen.Favorite.route) {
        UserAware(login) {
            FavoritePage {
                selectRoute(
                    Screen.FavoriteList.route.replace(
                        "{id}",
                        it.id
                    )
                )
            }
        }
    }
    composable(
        Screen.FavoriteList.route,
        arguments = listOf(navArgument("id") {
            type = NavType.StringType
        })
    ) {
        UserAware {
            FavoriteDetailPage(
                id = it.arguments?.getString("id").orEmpty()
            )
        }
    }
}