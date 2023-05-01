package com.storyteller_f.bi

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.storyteller_f.bi.components.FavoriteDetailPage
import com.storyteller_f.bi.components.FavoritePage
import com.storyteller_f.bi.components.HistoryPage
import com.storyteller_f.bi.components.HomeNavigation
import com.storyteller_f.bi.components.HomeTopBar
import com.storyteller_f.bi.components.MomentsPage
import com.storyteller_f.bi.components.NavItemIcon
import com.storyteller_f.bi.components.PlaylistPage
import com.storyteller_f.bi.components.Screen
import com.storyteller_f.bi.components.UserCenterDrawer
import com.storyteller_f.bi.components.VideoPage
import com.storyteller_f.bi.ui.theme.BiTheme
import com.storyteller_f.bi.unstable.userInfo
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.stream.IntStream

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val navController = rememberNavController()
            val selectRoute = { destination: String ->
                navController.navigate(destination) {
                    // Pop up to the start destination of the graph to
                    // avoid building up a large stack of destinations
                    // on the back stack as users select items
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    // Avoid multiple copies of the same destination when
                    // reselecting the same item
                    launchSingleTop = true
                    // Restore state when reselecting a previously selected item
                    restoreState = true
                }
            }


            val user by userInfo.observeAsState()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val items = Screen.allRoute.map {
                it.route
            }
            val context = LocalContext.current
            val currentRoute = navBackStackEntry?.destination?.hierarchy?.firstOrNull {
                items.contains(it.route)
            }?.route
            var adaptiveVideo by remember {
                mutableStateOf<String?>(null)
            }
            BiTheme {
                val calculateWindowSizeClass = calculateWindowSizeClass(this)
                when (calculateWindowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        CompatContent(userInfo = user, currentRoute, selectRoute) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.History.route
                            ) {
                                HomeNav(selectRoute) {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            VideoActivity::class.java
                                        ).apply {
                                            putExtra("videoId", it)
                                        })
                                }
                            }
                        }
                    }

                    WindowWidthSizeClass.Medium -> {
                        Row(modifier = Modifier.statusBarsPadding()) {
                            NavigationRail {
                                Screen.bottomNavigationItems.forEach {
                                    NavigationRailItem(
                                        selected = currentRoute == it.route,
                                        onClick = { selectRoute(it.route) },
                                        icon = { NavItemIcon(screen = it) })
                                }
                            }
                            NavHost(
                                navController = navController,
                                startDestination = Screen.History.route,
                                modifier = Modifier.weight(1f)
                            ) {
                                HomeNav(selectRoute) {
                                    adaptiveVideo = it
                                }
                            }
                            adaptiveVideo?.let {
                                Box(modifier = Modifier.weight(1f)) {
                                    VideoPage(it)
                                }
                            }
                        }
                    }

                    WindowWidthSizeClass.Expanded -> {
                        PermanentNavigationDrawer(drawerContent = {
                            PermanentDrawerSheet {
                                UserCenterDrawer()
                            }
                        }) {
                            Row(modifier = Modifier.statusBarsPadding()) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Screen.History.route
                                ) {
                                    HomeNav(selectRoute) {
                                        adaptiveVideo = it
                                    }
                                }
                                adaptiveVideo?.let {
                                    Box(modifier = Modifier.weight(1f)) {
                                        VideoPage(it)
                                    }
                                }
                            }

                        }
                    }
                }

            }
        }
    }

    @Composable
    private fun CompatContent(
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

    private fun NavGraphBuilder.HomeNav(
        selectRoute: (String) -> Unit,
        openVideo: (String) -> Unit
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

    @Composable
    private fun UserAware(content: @Composable () -> Unit) {
        val u by userInfo.observeAsState()
        val current = LocalContext.current
        if (u == null) {
            OneCenter {
                Button(onClick = {
                    current.startActivity(Intent(current, LoginActivity::class.java))
                }) {
                    Text(text = "login")
                }
            }
        } else {
            content()
        }
    }
}

@Composable
fun StandBy(width: Int, height: Int, me: @Composable () -> Unit) {
    StandBy(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp), me
    )
}

@Composable
fun StandBy(modifier: Modifier, me: @Composable () -> Unit) {
    val view = LocalView.current
    if (view.isInEditMode) {
        Box(modifier.background(MaterialTheme.colorScheme.primaryContainer))
    } else {
        me()
    }
}


fun String.createQRImage(width: Int, height: Int): Bitmap {
    val bitMatrix = QRCodeWriter().encode(
        this,
        BarcodeFormat.QR_CODE,
        width,
        height,
        Collections.singletonMap(EncodeHintType.CHARACTER_SET, "utf-8")
    )
    return Bitmap.createBitmap(
        IntStream.range(0, height).flatMap { h: Int ->
            IntStream.range(0, width).map { w: Int ->
                if (bitMatrix[w, h]
                ) Color.BLACK else Color.WHITE
            }
        }.toArray(),
        width, height, Bitmap.Config.ARGB_8888
    )
}
