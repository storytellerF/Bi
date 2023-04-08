package com.storyteller_f.bi.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import bilibili.app.interfaces.v1.HistoryGrpc
import bilibili.app.interfaces.v1.HistoryOuterClass
import com.a10miaomiao.bilimiao.comm.entity.comm.PaginationInfo
import com.a10miaomiao.bilimiao.comm.network.request
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.LoadingState
import kotlinx.coroutines.launch

fun MutableLiveData<LoadingState>.loaded() {
    value = LoadingState.Done
}

fun MutableLiveData<LoadingState>.error(e: Exception) {
    value = LoadingState.Error(e)
}

fun MutableLiveData<LoadingState>.loading(message: String = "") {
    value = LoadingState.Loading(message)
}

class HistoryViewModel : ViewModel() {
    var list = PaginationInfo<HistoryOuterClass.CursorItem>()
    val state = MutableLiveData<LoadingState>()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.loading()
            try {
                val req = HistoryOuterClass.CursorV2Req.newBuilder().apply {
                    business = "archive"
                    cursor = HistoryOuterClass.Cursor.newBuilder().apply {

                    }.build()
                }.build()
                val res = HistoryGrpc.getCursorV2Method()
                    .request(req)
                    .awaitCall()
                list.data.addAll(res.itemsList)
                state.loaded()
            } catch (e: Exception) {
                state.error(e)
            }

        }

    }
}

@Composable
fun HistoryPage() {
    val viewModel = viewModel<HistoryViewModel>()
    val observeAsState by viewModel.state.observeAsState()
    val list = viewModel.list
    when (val state = observeAsState) {
        is LoadingState.Loading -> Text(text = "loading")
        is LoadingState.Error -> Text(text = state.e.localizedMessage ?: "")
        is LoadingState.Done -> LazyColumn {
            items(list.data.size) {
                val cursorItem = list.data[it]
                HistoryItem(cursorItem)
            }
        }

        else -> Text(text = "impossible")
    }
}

class VideoItemProvider : PreviewParameterProvider<HistoryOuterClass.CursorItem> {
    override val values: Sequence<HistoryOuterClass.CursorItem>
        get() = sequence {
            yield(HistoryOuterClass.CursorItem.getDefaultInstance())
            yield(HistoryOuterClass.CursorItem.getDefaultInstance())
        }

}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HistoryItem(
    @PreviewParameter(VideoItemProvider::class) item: HistoryOuterClass.CursorItem,
) {
    val text = item.title
    val text1 = item.dt.type.name
    VideoItem(item.cover(), text, text1)
}

@Preview
@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun VideoItem(
    url: String? = null,
    text: String = "text",
    label: String = "label"
) {

    Row(modifier = Modifier.padding(8.dp)) {
        val coverModifier = Modifier
            .width(80.dp)
            .height(45.dp)
        if (url == null) {
            Box(coverModifier.background(Color.Blue))
        } else {
            val u = "${UrlUtil.autoHttps(url)}@672w_378h_1c_"
            GlideImage(u, contentDescription = null, modifier = coverModifier)
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = text)
            Text(text = label)
        }
    }
}

fun HistoryOuterClass.CursorItem.cover(): String {
    return if (hasCardOgv()) {
        cardOgv.cover
    } else {
        cardUgc.cover
    }
}