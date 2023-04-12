package com.storyteller_f.bi.components

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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

class QrcodeLoginViewModel(private val context: Application) : AndroidViewModel(context) {
    val state = MutableLiveData<LoadingState>()
    val qrcodeUrl = MutableLiveData<String>()
    val checkState = MutableLiveData<LoadingState>()
    var currentAuthCode: String? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                state.value = LoadingState.Loading("")
                val res = BiliApiService.authApi
                    .qrCode()
                    .awaitCall()
                    .gson<ResultInfo<QRLoginInfo>>()
                val data = res.data
                if (res.isSuccess && data.url.isNotEmpty() && data.auth_code.isNotEmpty()) {
                    qrcodeUrl.value = data.url
                    currentAuthCode = data.auth_code
                    state.loaded()
                    checkQr(data.auth_code)
                } else {
                    state.error(Exception(res.message))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                state.error(e)
            }

        }
    }

    private fun checkQr(authCode: String) {
        if (currentAuthCode != authCode) return
        checkState.loading("等待扫码")
        viewModelScope.launch {
            val res = BiliApiService.authApi.checkQrCode(authCode)
                .awaitCall()
                .gson<ResultInfo<LoginInfo.QrLoginInfo>>()
            when (res.code) {
                86039 -> {
                    checkState.loading("未确认")
                    // 未确认
                    delay(3000)
                    checkQr(authCode)
                }

                86090 -> {
                    checkState.loading("扫描成功，请点击确认")
                    // 已扫码未确认
                    delay(2000)
                    checkQr(authCode)
                }

                86038, -3 -> {
                    // 过期、失效
                    checkState.loading("二维码已过期，请刷新")
                }

                0 -> {
                    checkState.loading("扫码成功，正在读取信息")
                    // 成功
                    val loginInfo = res.data.toLoginInfo()
                    BilimiaoCommApp.commApp.saveAuthInfo(loginInfo)
                    getUserInfo()
                }

                else -> {
                    // 发生错误
                    checkState.loading("登录失败，请稍后重试\n" + res.message)
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
            LoadingState.Error(Exception(res.message))
        }

    }
}

data class LoginState(
    val qrcodeUrl: String?,
    val loadingState: LoadingState?,
    val checkState: LoadingState?
)

class LoginPreviewProvider : PreviewParameterProvider<LoginState> {
    override val values: Sequence<LoginState>
        get() = sequence {
            yield(LoginState("hello", LoadingState.Done, LoadingState.Done))
            yield(LoginState(null, LoadingState.Loading("loading"), null))
        }
}

@Preview
@Composable
fun LoginInternal(
    @PreviewParameter(LoginPreviewProvider::class) state: LoginState
) {
    val (qrcodeUrl, loadingState, checkState) = state
    val image = (qrcodeUrl ?: "qrcode not ready").createQRImage(200, 200)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        val widthDp = LocalConfiguration.current.smallestScreenWidthDp - 100
        Box(contentAlignment = Alignment.Center) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "test",
                modifier = Modifier
                    .width(
                        widthDp.dp
                    )
                    .height(widthDp.dp)
            )
            if (loadingState !is LoadingState.Done) {
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                        .widthIn(max = widthDp.dp),
                    text = when (loadingState) {
                        null -> "waiting"
                        is LoadingState.Error -> loadingState.e.localizedMessage.orEmpty()
                        is LoadingState.Loading -> loadingState.state
                        else -> "impossible"
                    }
                )
            }
        }
        if (checkState != null) {
            Text(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(8.dp),
                text = when (checkState) {
                    is LoadingState.Done -> "扫码成功"
                    is LoadingState.Loading -> checkState.state
                    is LoadingState.Error -> checkState.e.localizedMessage
                }
            )
        }
    }
}

@Composable
fun LoginPage() {
    val loginViewModel = viewModel<QrcodeLoginViewModel>()
    val loadingState by loginViewModel.state.observeAsState()
    val url by loginViewModel.qrcodeUrl.observeAsState()
    val checkState by loginViewModel.checkState.observeAsState()
    LoginInternal(LoginState(url, loadingState, checkState))
}