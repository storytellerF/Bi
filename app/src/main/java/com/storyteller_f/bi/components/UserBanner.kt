package com.storyteller_f.bi.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.storyteller_f.bi.defaultFactory
import com.storyteller_f.bi.unstable.userInfo
import kotlinx.coroutines.launch

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
    val modifier = Modifier
        .padding(start = 16.dp)
        .fillMaxWidth()
    if (u != null) {
        val face = UrlUtil.autoHttps(u.face)
        Column {
            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                StandBy(coverSize) {
                    GlideImage(
                        model = face, contentDescription = "avatar", modifier = coverSize
                    )
                }
                Text(
                    text = u.name,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.size(8.dp))
                Dot(s = if (u.sex == 1) "M" else "F")
                Spacer(modifier = Modifier.size(8.dp))
                Dot(u.level.toString())
            }
            val v = viewModel<UserBannerViewModel>(factory = defaultFactory)
            val info by v.data.observeAsState()
            Text(text = info?.card?.sign ?: "不说两句？", modifier = Modifier.padding(8.dp))
            Row(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "follower ${u.follower}", modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp), color = MaterialTheme.colorScheme.onSecondary
                )
                Text(
                    text = "following ${u.following}", modifier = Modifier
                        .padding(start = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp), color = MaterialTheme.colorScheme.onSecondary
                )
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

@Composable
private fun Dot(s: String = "") {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                MaterialTheme.colorScheme.tertiary,
                RoundedCornerShape(16.dp)
            ), contentAlignment = Alignment.Center
    ) {
        Text(text = s, fontSize = 16.sp, color = MaterialTheme.colorScheme.onTertiary)
    }
}

class UserBannerViewModel : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    val data = MutableLiveData<SpaceInfo?>()

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
                val gson = BiliApiService.userApi.space(mid.toString()).awaitCall()
                    .gson<ResultInfo<SpaceInfo>>()
                if (gson.isSuccess) {
                    data.value = gson.data
                    state.loaded()
                } else {
                    state.error(gson.error())
                }
            } catch (e: Throwable) {
                state.error(e)
            }
        }
    }

}
