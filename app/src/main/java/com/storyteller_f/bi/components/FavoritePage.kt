package com.storyteller_f.bi.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.a10miaomiao.bilimiao.comm.entity.ListAndCountInfo
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediaListInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.unstable.userInfo

@Composable
fun FavoritePage(openMediaList: (MediaListInfo) -> Unit = {}) {
    val favoriteViewModel = viewModel<FavoriteViewModel>()
    val pagingItems = favoriteViewModel.flow.collectAsLazyPagingItems()
    StateView(pagingItems) {
        LazyVerticalGrid(GridCells.Adaptive(150.dp)) {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey(),
                contentType = pagingItems.itemContentType()
            ) {
                pagingItems[it]?.let { info -> MediaListContainer(info, openMediaList) }
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
fun MediaListContainer(
    @PreviewParameter(MediaListContainerPreviewProvider::class) mediaListInfo: MediaListInfo,
    openMediaList: (MediaListInfo) -> Unit = {}
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable {
        openMediaList(mediaListInfo)
    }) {
        val modifier = Modifier.aspectRatio(16f / 9)
        StandBy(modifier) {
            val u = UrlUtil.autoHttps(mediaListInfo.cover)
            GlideImage(
                model = "$u@672w_378h_1c_", contentDescription = "cover", modifier = modifier
            )
        }

        Text(
            text = mediaListInfo.title, modifier = Modifier
                .background(Color.White)
                .padding(16.dp)
        )
    }
}

class FavoriteViewModel : PagingViewModel<Int, MediaListInfo>({
    FavoriteSource(userInfo.value?.mid?.toString()?.trim().orEmpty())
})

class FavoriteSource(private val mid: String) : PagingSource<Int, MediaListInfo>() {
    override fun getRefreshKey(state: PagingState<Int, MediaListInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaListInfo> {
        if (mid.isEmpty()) {
            return loadResultError("mid 不合法")
        }
        val key = params.key ?: 1
        return try {
            val res = BiliApiService.userApi.favFolderList(
                mid, pageNum = key, pageSize = params.loadSize
            ).awaitCall().gson<ResultInfo<ListAndCountInfo<MediaListInfo>>>()
            if (!res.isSuccess) {
                res.loadResultError()
            } else {
                val data = res.data
                LoadResult.Page(
                    data.list, prevKey = null, nextKey = if (data.has_more) key + 1 else null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    }

}

fun <K : Any, V : Any> loadResultError(error: String): PagingSource.LoadResult.Error<K, V> {
    return PagingSource.LoadResult.Error(Exception(error))
}