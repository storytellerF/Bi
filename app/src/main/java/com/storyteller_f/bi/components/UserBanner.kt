package com.storyteller_f.bi.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.storyteller_f.bi.userInfo
import kotlinx.coroutines.launch

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCenterDrawer(userInfo: UserInfo? = null) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))

        UserBanner(modifier = Modifier.padding(start = 16.dp), userInfo)
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(label = { Text(text = "Setting") }, icon = {
            Icon(Icons.Filled.Settings, contentDescription = "setting")
        }, selected = false, onClick = { /*TODO*/ })
        NavigationDrawerItem(label = { Text(text = "Logout") }, icon = {
            Icon(Icons.Filled.Close, contentDescription = "logout")
        }, selected = false, onClick = { /*TODO*/ })
    }
}

@Preview
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UserBanner(modifier: Modifier = Modifier, u: UserInfo? = null) {
    val coverSize = Modifier
        .width(60.dp)
        .height(60.dp)
    if (u != null) {
        val face = UrlUtil.autoHttps(u.face)
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            GlideImage(
                model = face, contentDescription = "avatar", modifier = coverSize
            )
            Text(text = u.name, modifier = Modifier.padding(start = 8.dp))
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = coverSize.background(Color.Blue))
            Button(modifier = Modifier.padding(start = 8.dp), onClick = {

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
            } catch (e: Exception) {
                state.error(e)
            }
        }
    }

}