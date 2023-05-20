package com.storyteller_f.bi.components

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.storyteller_f.bi.UserAware
import com.storyteller_f.bi.unstable.logout
import kotlinx.coroutines.launch

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


@Composable
private fun HomeDrawerContent(userInfo: UserInfo?) {
    ModalDrawerSheet {
        val context = LocalContext.current
        Spacer(Modifier.height(12.dp))
        UserBanner(u = userInfo)
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(label = { Text(text = "Setting") }, icon = {
            Icon(Icons.Filled.Settings, contentDescription = "setting")
        }, selected = false, onClick = {
            Toast.makeText(context, "not implementation", Toast.LENGTH_SHORT).show()
        })
        NavigationDrawerItem(label = { Text(text = "Logout") }, icon = {
            Icon(Icons.Filled.Close, contentDescription = "logout")
        }, selected = false, onClick = {
            context.logout()
        })
    }
}

fun NavGraphBuilder.homeNav(
    selectRoute: (String) -> Unit,
    openVideo: (String, Long) -> Unit
) {
    composable(Screen.History.route) {
        UserAware {
            HistoryPage(openVideo)
        }
    }
    composable(Screen.Moments.route) {
        UserAware {
            MomentsPage(openVideo)
        }
    }
    composable(Screen.Playlist.route) {
        UserAware {
            PlaylistPage(openVideo)
        }
    }
    composable(Screen.Favorite.route) {
        UserAware {
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