package com.storyteller_f.bi

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.storyteller_f.bi.components.HistoryPage
import com.storyteller_f.bi.ui.theme.BiTheme
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.stream.IntStream

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()
            val open = {
                coroutineScope.launch {
                    drawerState.open()
                }
            }
            val user by userInfo.observeAsState()
            val u = user
            BiTheme {
                ModalNavigationDrawer(drawerContent = {
                    if (u != null) {
                        Text(text = u.name)
                    } else Button(onClick = {

                    }) {
                        Text(text = "login")
                    }
                }, drawerState = drawerState) {
                    Scaffold(topBar = {
                        TopAppBar(
                            title = {
                                Text(text = "Bi")
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    open()
                                }) {
                                    Icon(Icons.Filled.Menu, contentDescription = null)
                                }
                            },
                        )
                    }) {
                        Surface(
                            modifier = Modifier.fillMaxSize().padding(it),
                            color = MaterialTheme.colorScheme.background
                        ) {
//                            LoginPage(url, loadingState, checkState)
                            HistoryPage()
                        }
                    }

                }

            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val image by remember {
        derivedStateOf {
            "hello".createQRImage(200, 200)
        }
    }
    val widthDp = LocalConfiguration.current.smallestScreenWidthDp - 100

    Column {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(onClick = { /*TODO*/ }) {
            Text(text = "request")
        }
        Button(onClick = { /*TODO*/ }) {
            Text(text = "get")
        }
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = "test",
            modifier = Modifier
                .width(
                    widthDp.dp
                )
                .height(widthDp.dp)
        )
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BiTheme {
        Greeting("Android")
    }
}