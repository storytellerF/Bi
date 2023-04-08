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

class HistoryViewModel : ViewModel() {
    var list = PaginationInfo<HistoryOuterClass.CursorItem>()
    val state = MutableLiveData<LoadingState>()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.value = LoadingState.Loading("")
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
                state.value = LoadingState.Done
            } catch (e: Exception) {
                state.value = LoadingState.Error(e)
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
                HistoryItem(cursorItem, editMode = false)
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

@Preview
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HistoryItem(
    @PreviewParameter(VideoItemProvider::class) item: HistoryOuterClass.CursorItem,
    editMode: Boolean = true
) {

    Row(modifier = Modifier.padding(8.dp)) {
        val coverModifier = Modifier
            .width(80.dp)
            .height(45.dp)
        if (editMode) {
            Box(coverModifier.background(Color.Blue))
        } else {
            val url = UrlUtil.autoHttps(item.cover())
            val u = "$url@672w_378h_1c_"
            GlideImage(u, contentDescription = null, modifier = coverModifier)
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = item.title)
            Text(text = item.dt.type.name)
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