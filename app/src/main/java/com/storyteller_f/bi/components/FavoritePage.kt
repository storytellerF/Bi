package com.storyteller_f.bi.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
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
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediaListInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserSpaceFavFolderInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.LoadingState
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.unstable.userInfo
import kotlinx.coroutines.launch

@Composable
fun FavoritePage(openMediaList: (MediaListInfo) -> Unit = {}) {
    val favoriteViewModel = viewModel<FavoriteViewModel>()
    val state by favoriteViewModel.state.observeAsState()
    val data by favoriteViewModel.data.observeAsState()
    StateView(state = state) {
        LazyVerticalGrid(GridCells.Adaptive(150.dp)) {
            data?.default_folder?.folder_detail?.let {
                item {
                    MediaListContainer(it, openMediaList)
                }
            }
            data?.space_infos?.forEach {
                item(span = {
                    GridItemSpan(maxLineSpan)
                }) {
                    Text(text = "${it.id} - ${it.name} - ${it.mediaListResponse.count}")
                }
                it.mediaListResponse.list?.let { list ->
                    items(list) { info ->
                        MediaListContainer(mediaListInfo = info, openMediaList)
                    }
                }
            }
        }
    }
}

class MediaListContainerPreviewProvider : PreviewParameterProvider<MediaListInfo> {
    override val values: Sequence<MediaListInfo>
        get() = sequence {
            yield(MediaListInfo("", "intro", "title", 1, 0L, 1, 0L, "", 1, 1, 0L, 0L, 1, 1))
        }

}

@Preview
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaListContainer(@PreviewParameter(MediaListContainerPreviewProvider::class) mediaListInfo: MediaListInfo, openMediaList: (MediaListInfo) -> Unit = {}) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable {
        openMediaList(mediaListInfo)
    }) {
        val modifier = Modifier.aspectRatio(16f / 9)
        StandBy(modifier) {
            val u = UrlUtil.autoHttps(mediaListInfo.cover)
            GlideImage(
                model = "$u@672w_378h_1c_",
                contentDescription = "cover",
                modifier = modifier
            )
        }

        Text(
            text = mediaListInfo.title, modifier = Modifier
                .background(Color.White)
                .padding(16.dp)
        )
    }
}

class FavoriteViewModel : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    val data = MutableLiveData<UserSpaceFavFolderInfo>()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            state.loading()
            try {
                val mid = userInfo.value?.mid?.toString().orEmpty()
                if (mid.isNotEmpty()) {
                    val res = BiliApiService.userApi.favFolderList(mid).awaitCall()
                        .gson<ResultInfo<UserSpaceFavFolderInfo>>()
                    if (res.code == 0) {
                        val info = res.data
                        data.value = info
                        state.loaded()
                    } else {
                        state.error(res.message)
                    }
                } else {
                    state.error("找不到mid")
                }
            } catch (e: Throwable) {
                state.error(e)
            }
        }

    }
}


