package com.storyteller_f.bi.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediaDetailInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediasInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.defaultFactory
import com.storyteller_f.bi.playVideo

object FavoriteIdKey : CreationExtras.Key<String>

@Composable
fun FavoriteDetailPage(id: String) {
    val current = LocalContext.current
    val detailViewModel = viewModel<FavoriteDetailViewModel>(
        factory = defaultFactory,
        extras = MutableCreationExtras().apply {
            set(FavoriteIdKey, id)
        })
    val lazyPagingItems = detailViewModel.flow.collectAsLazyPagingItems()
    StateView(state = lazyPagingItems.loadState.refresh) {
        LazyColumn {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey(),
                contentType = lazyPagingItems.itemContentType(
                )
            ) { index ->
                val item = lazyPagingItems[index]
                VideoItem(
                    item?.cover.orEmpty(),
                    item?.title.orEmpty(),
                    item?.upper?.name.orEmpty()
                ) {
                    current.playVideo(item?.id, item?.id, "archive", 0)
                }
            }
        }
    }

}

class FavoriteDetailViewModel(id: String) : ViewModel() {
    val flow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        FavoriteDetailSource(id)
    }.flow
        .cachedIn(viewModelScope)
}

class FavoriteDetailSource(val id: String) : PagingSource<Int, MediasInfo>() {
    override fun getRefreshKey(state: PagingState<Int, MediasInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediasInfo> {
        if (id.isEmpty()) {
            return LoadResult.Error(Exception("id is empty"))
        }
        val lastPage = params.key ?: 0
        val currentPage = lastPage + 1
        val pageSize = 20
        val res = BiliApiService.userApi
            .mediaDetail(
                media_id = id,
                pageNum = currentPage,
                pageSize = pageSize,
            )
            .awaitCall()
            .gson<ResultInfo<MediaDetailInfo>>()
        return if (res.code == 0) {
            val data = res.data
            val medias = data.medias.orEmpty()
            LoadResult.Page(
                medias,
                null,
                if (medias.size < pageSize) null else currentPage + 1
            )
        } else {
            LoadResult.Error(res.error())
        }
    }

}