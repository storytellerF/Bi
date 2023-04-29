package com.storyteller_f.bi.components

import android.content.Intent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import bilibili.app.archive.v1.Archive
import bilibili.app.dynamic.v2.Stat
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.VideoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistPage() {
    val context = LocalContext.current
    val viewModel = viewModel<ToBePlayedViewModel>()
    val state by viewModel.state.observeAsState()
    val list by viewModel.datum.observeAsState()
    val data = list?.list.orEmpty()
    StateView(state = state) {
        LazyColumn {
            items(data, {
                it.aid.toString() + " " + it.bvid
            }) {
                VideoItem(it.pic, it.title, "${it.aid} ${it.bvid} ${it.cid} ${it.tid}") {
                    context.startActivity(Intent(context, VideoActivity::class.java).apply {
                        putExtra("videoId", it.bvid)
                    })
                }
            }
        }
    }
}

class VideoDatumList(
    val count: Int,
    val list: List<VideoDatum>
)

class ToBePlayedViewModel : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    val datum = MutableLiveData<VideoDatumList?>()

    init {
        load()
    }

    private fun load() {
        state.loading()
        viewModelScope.launch {
            try {

                val gson = withContext(Dispatchers.IO) {
                    BiliApiService.userApi.toBePlay().awaitCall()
                        .gson<ResultInfo<VideoDatumList>>()
                }
                if (gson.code == 0) {
                    datum.value = gson.data
                    state.loaded()
                } else {
                    state.error(Exception("code: ${gson.code} ${gson.message}"))
                }
            } catch (e: Throwable) {
                state.error(e)
            }
        }
    }
}

data class VideoDatum(
    val aid: Long,
    val videos: Int,
    /**
     * 分区id
     */
    val tid: Long,
    val tname: String,
    val copyright: Int,
    val pic: String,
    val title: String,
    val pubdate: Int,
    val ctime: Int,
    val desc: String,
    val state: Int,
    val duration: Int,
    val rights: Archive.Rights,
    val owner: Archive.Author,
    val stat: Stat,
    val dynamic: String,
    val dimension: Archive.Dimension,
    val count: Int,
    val cid: Int,
    val progress: Int,
    val add_at: Int,
    val bvid: String,
)