package com.storyteller_f.bi.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.user.SpaceInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.LoginActivity
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.unstable.logout
import com.storyteller_f.bi.unstable.userInfo
import kotlinx.coroutines.launch

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCenterDrawer(userInfo: UserInfo? = null) {
    val current = LocalContext.current
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        UserBanner(userInfo)
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(label = { Text(text = "Setting") }, icon = {
            Icon(Icons.Filled.Settings, contentDescription = "setting")
        }, selected = false, onClick = {
            Toast.makeText(current, "not implementation", Toast.LENGTH_SHORT).show()
        })
        NavigationDrawerItem(label = { Text(text = "Logout") }, icon = {
            Icon(Icons.Filled.Close, contentDescription = "logout")
        }, selected = false, onClick = {
            current.logout()
        })
    }
}

class UserBannerPreviewProvider : PreviewParameterProvider<UserInfo?> {
    override val values: Sequence<UserInfo?>
        get() = sequence {
            yield(null)
            yield(UserInfo(0, "storyteller f", "", "", 50.0, 50.0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
        }

}

@Preview
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UserBanner(@PreviewParameter(UserBannerPreviewProvider::class) u: UserInfo?) {
    val coverSize = Modifier
        .size(60.dp)
    val modifier = Modifier.padding(start = 16.dp).fillMaxWidth()
    if (u != null) {
        val face = UrlUtil.autoHttps(u.face)
        Column {
            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                StandBy(coverSize) {
                    GlideImage(
                        model = face, contentDescription = "avatar", modifier = coverSize
                    )
                }
                Text(text = u.name, modifier = Modifier.padding(start = 8.dp))
                Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text(text = if( u.sex == 1) "F" else "M", fontSize = 8.sp)
                }
            }
            Row(modifier = Modifier.padding(8.dp)) {
                Text(text = "follower ${u.follower}", modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).padding(8.dp))
                Text(text = "following ${u.following}", modifier = Modifier.padding(start = 8.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).padding(8.dp))
            }
        }
    } else {
        val current = LocalContext.current
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = coverSize.background(Color.Blue))
            Button(modifier = Modifier.padding(start = 8.dp), onClick = {
                current.startActivity(Intent(current, LoginActivity::class.java))
            }) {
                Text(text = "login")
            }
        }
    }
}

class UserBannerViewModel() : ViewModel() {
    val state = MutableLiveData<LoadingState>()

    init {
        load()
    }

    private fun load() {
        state.loading()
        val mid = userInfo.value?.mid
        if (mid == null || mid == 0L) {
            state.error(Exception("未登录"))
            return
        }
        viewModelScope.launch {
            try {
                BiliApiService.userApi.space(mid.toString()).awaitCall()
                    .gson<ResultInfo<SpaceInfo>>()
                state.loaded()
            } catch (e: Throwable) {
                state.error(e)
            }
        }
    }

}
