package com.storyteller_f.bi.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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


@Preview
@Composable
fun ExpandedContent(
    adaptiveVideo: String? = null,
    initProgress: Long = 0L,
    content: @Composable () -> Unit = {}
) {
    PermanentNavigationDrawer(drawerContent = {
        PermanentDrawerSheet {
            UserCenterDrawer()
        }
    }) {
        Row(modifier = Modifier.statusBarsPadding()) {
            Column(modifier = Modifier.weight(1f)) {
                SearchPage(modifier = Modifier.padding(horizontal = 8.dp), dockMode = true) {

                }
                content()
            }
            adaptiveVideo?.let {
                Box(modifier = Modifier.weight(1f)) {
                    VideoPage(it, initProgress)
                }
            }
        }

    }
}

@Composable
fun MediumContent(
    currentRoute: String? = Screen.History.route,
    adaptiveVideo: String? = null,
    initProgress: Long = 0L,
    selectRoute: (String) -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Row(modifier = Modifier.statusBarsPadding()) {
        NavigationRail {
            Screen.bottomNavigationItems.forEach {
                NavigationRailItem(
                    selected = currentRoute == it.route,
                    onClick = { selectRoute(it.route) },
                    icon = { NavItemIcon(screen = it) })
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            SearchPage(modifier = Modifier.padding(horizontal = 8.dp), dockMode = true) {

            }
            content()
        }
        adaptiveVideo?.let {
            Box(modifier = Modifier.weight(1f)) {
                VideoPage(it, initProgress)
            }
        }
    }
}

@Composable
fun CompatContent(
    userInfo: UserInfo?,
    currentRoute: String?,
    selectRoute: (String) -> Unit = {},
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val open = {
        coroutineScope.launch {
            drawerState.open()
        }
    }
    ModalNavigationDrawer(
        drawerContent = {
            UserCenterDrawer(userInfo = userInfo)
        },
        drawerState = drawerState
    ) {
        Scaffold(topBar = {
            HomeTopBar {
                open()
            }
        }, bottomBar = {
            HomeNavigation(currentRoute, selectRoute)
        }) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }

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
            MomentsPage()
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