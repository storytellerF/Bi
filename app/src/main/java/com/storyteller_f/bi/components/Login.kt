package com.storyteller_f.bi.components

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.LoginInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.QRLoginInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.createQRImage
import com.storyteller_f.bi.saveUserInfo
import com.storyteller_f.bi.userInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QrcodeLoginViewModel(val context: Application) : AndroidViewModel(context) {
    val state = MutableLiveData<LoadingState>()
    val url = MutableLiveData<String>()
    val checkState = MutableLiveData<LoadingState>()
    var currentAuthCode: String? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                state.value = LoadingState.Loading("")
                val res = BiliApiService.authApi
                    .qrCode()
                    .awaitCall()
                    .gson<ResultInfo<QRLoginInfo>>()
                if (res.isSuccess) {
                    val data = res.data
                    url.value = data.url
                    currentAuthCode = data.auth_code
                    state.value = LoadingState.Done
                    checkState.value = LoadingState.Loading("等待扫码")
                    checkQr(data.auth_code)
                } else {
                    state.value = LoadingState.Error(java.lang.Exception(res.message))
                }
            } catch (e: Exception) {
                state.value = LoadingState.Error(e)
            }

        }
    }

    fun checkQr(authCode: String) {
        if (currentAuthCode != authCode) return
        viewModelScope.launch {
            val res = BiliApiService.authApi.checkQrCode(authCode)
                .awaitCall()
                .gson<ResultInfo<LoginInfo.QrLoginInfo>>()
            when (res.code) {
                86039 -> {
                    checkState.value = LoadingState.Loading("未确认")
                    // 未确认
                    delay(3000)
                    checkQr(authCode)
                }

                86090 -> {
                    checkState.value = LoadingState.Loading("扫描成功，请点击确认")
                    // 已扫码未确认
                    delay(2000)
                    checkQr(authCode)
                }

                86038, -3 -> {
                    // 过期、失效
                    checkState.value = LoadingState.Loading("二维码已过期，请刷新")
                }

                0 -> {
                    checkState.value = LoadingState.Loading("扫码成功，正在读取信息")
                    // 成功
                    val loginInfo = res.data.toLoginInfo()
                    BilimiaoCommApp.commApp.saveAuthInfo(loginInfo)
                    getUserInfo()
                }

                else -> {
                    // 发生错误
                    checkState.value = LoadingState.Loading("登录失败，请稍后重试\n" + res.message)
                }
            }
        }
    }

    private suspend fun getUserInfo() {
        val res =
            BiliApiService.authApi
                .account()
                .awaitCall()
                .gson<ResultInfo<UserInfo>>()
        checkState.value = if (res.isSuccess) {
            userInfo.value = res.data
            context.saveUserInfo(res.data)
            LoadingState.Done
        } else {
            LoadingState.Error(java.lang.Exception(res.message))
        }

    }
}

@Composable
fun LoginInternal(
    url: String?,
    loadingState: LoadingState?,
    checkState: LoadingState?
) {
    val image by remember {
        derivedStateOf {
            (url ?: "url not ready").createQRImage(200, 200)
        }
    }
    Column {
        Text(
            text = when (loadingState) {
                is LoadingState.Error -> loadingState.e.localizedMessage
                is LoadingState.Done -> url.toString()
                else -> loadingState.toString()
            }
        )
        val ls = checkState
        Text(
            text = when (ls) {
                is LoadingState.Done -> "done"
                is LoadingState.Loading -> ls.state
                is LoadingState.Error -> ls.e.localizedMessage
                else -> "else"
            }
        )
        val widthDp = LocalConfiguration.current.smallestScreenWidthDp - 100
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

@Composable
fun LoginPage() {
    val loginViewModel = viewModel<QrcodeLoginViewModel>()
    val loadingState by loginViewModel.state.observeAsState()
    val url by loginViewModel.url.observeAsState()
    val checkState by loginViewModel.checkState.observeAsState()
    LoginInternal(url, loadingState, checkState)
}