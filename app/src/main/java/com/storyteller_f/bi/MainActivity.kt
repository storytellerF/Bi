package com.storyteller_f.bi

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.storyteller_f.bi.components.FavoriteDetailPage
import com.storyteller_f.bi.components.FavoritePage
import com.storyteller_f.bi.components.HistoryPage
import com.storyteller_f.bi.components.HomeNavigation
import com.storyteller_f.bi.components.HomeTopBar
import com.storyteller_f.bi.components.MomentsPage
import com.storyteller_f.bi.components.PlaylistPage
import com.storyteller_f.bi.components.Screen
import com.storyteller_f.bi.components.UserCenterDrawer
import com.storyteller_f.bi.ui.theme.BiTheme
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.stream.IntStream

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val selectRoute = { screen: Screen ->
                navController.navigate(screen.route) {
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

            val open = {
                coroutineScope.launch {
                    drawerState.open()
                }
            }
            val user by userInfo.observeAsState()
            val u = user

            BiTheme {
                ModalNavigationDrawer(
                    drawerContent = {
                        UserCenterDrawer(userInfo = u)
                    },
                    drawerState = drawerState
                ) {
                    Scaffold(topBar = {
                        HomeTopBar {
                            open()
                        }
                    }, bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val items = Screen.allRoute.map {
                            it.route
                        }
                        val any = navBackStackEntry?.destination?.hierarchy?.firstOrNull {
                            items.contains(it.route)
                        }?.route
                        HomeNavigation(any, selectRoute)
                    }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.History.route
                            ) {
                                composable(Screen.History.route) {
                                    UserAware {
                                        HistoryPage()
                                    }
                                }
                                composable(Screen.Moments.route) {
                                    UserAware {
                                        MomentsPage()
                                    }
                                }
                                composable(Screen.Playlist.route) {
                                    UserAware {
                                        PlaylistPage()
                                    }
                                }
                                composable(Screen.Favorite.route) {
                                    UserAware {
                                        FavoritePage {
                                            navController.navigate(Screen.FavoriteList.route.replace("{id}", it.id))
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
                                        FavoriteDetailPage(id = it.arguments?.getString("id").orEmpty())
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
