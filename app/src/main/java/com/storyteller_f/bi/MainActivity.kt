package com.storyteller_f.bi

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.storyteller_f.bi.components.HistoryPage
import com.storyteller_f.bi.components.HomeTopBar
import com.storyteller_f.bi.components.MomentsPage
import com.storyteller_f.bi.components.Screen
import com.storyteller_f.bi.components.ToBePlayedPage
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

            val open = {
                coroutineScope.launch {
                    drawerState.open()
                }
            }
            val user by userInfo.observeAsState()
            val u = user
            val items = listOf(
                Screen.History,
                Screen.Moments,
                Screen.ToBePlay
            )
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
                        val currentDestination = navBackStackEntry?.destination
                        NavigationBar {
                            items.forEach { screen ->
                                val selected =
                                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                NavigationBarItem(selected = selected, onClick = {
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
                                composable(Screen.ToBePlay.route) {
                                    UserAware {
                                        ToBePlayedPage()
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
    val view = LocalView.current
    if (view.isInEditMode) {
        Box(modifier = Modifier
            .width(width.dp)
            .height(height.dp))
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
