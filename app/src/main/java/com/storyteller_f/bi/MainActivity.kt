package com.storyteller_f.bi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.storyteller_f.bi.components.CompatContent
import com.storyteller_f.bi.components.ExpandedContent
import com.storyteller_f.bi.components.MediumContent
import com.storyteller_f.bi.components.Screen
import com.storyteller_f.bi.components.SearchPage
import com.storyteller_f.bi.components.homeNav
import com.storyteller_f.bi.ui.theme.BiTheme
import com.storyteller_f.bi.unstable.userInfo
import java.util.Collections
import java.util.stream.IntStream

class MainActivity : ComponentActivity() {
    companion object

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
            var initProgress by remember {
                mutableStateOf(0L)
            }
            var searchInCompat by rememberSaveable {
                mutableStateOf(false)
            }
            BiTheme {
                val calculateWindowSizeClass = calculateWindowSizeClass(this)
                val openParallelVideo: (String, Long) -> Unit = { it, progress ->
                    adaptiveVideo = it
                    initProgress = progress
                }

                when (calculateWindowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {

                        Box {
                            CompatContent(userInfo = user, currentRoute, selectRoute, search = {
                                searchInCompat = true
                            }) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Screen.History.route
                                ) {
                                    homeNav(selectRoute) { it, progress ->
                                        context.playVideo(it, progress)
                                    }
                                }
                            }
                            AnimatedVisibility(
                                visible = searchInCompat,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Surface {
                                    SearchPage(
                                        modifier = Modifier.fillMaxWidth(),
                                        instantActive = true
                                    ) {
                                        searchInCompat = false
                                    }
                                }
                            }
                        }
                    }

                    WindowWidthSizeClass.Medium -> {
                        MediumContent(currentRoute, adaptiveVideo, initProgress, selectRoute) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.History.route,
                            ) {
                                homeNav(selectRoute, openParallelVideo)
                            }
                        }
                    }

                    WindowWidthSizeClass.Expanded -> {
                        ExpandedContent(adaptiveVideo, initProgress) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.History.route
                            ) {
                                homeNav(selectRoute, openParallelVideo)
                            }
                        }
                    }
                }

            }
        }
    }

}

@Composable
fun UserAware(content: @Composable () -> Unit) {
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

fun Context.playVideo(it: String?, progress: Long = 0L) {
    startActivity(
        Intent(
            this,
            VideoActivity::class.java
        ).apply {
            putExtra("videoId", it)
            putExtra("progress", progress)
        })
}