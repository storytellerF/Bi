package com.storyteller_f.bi.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.a10miaomiao.bilimiao.comm.entity.search.SearchArchiveInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchBangumiInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchListInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchResultInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchUpperInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchVideoInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.playVideo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchPage(back: () -> Unit) {
    val current = LocalContext.current
    val viewModel = viewModel<VideoSearchViewModel>(factory = defaultFactory)

    val query by viewModel.keyword.collectAsState()
    SearchBar(query = query, onQueryChange = {
        viewModel.keyword.value = it
    }, onSearch = {
    }, active = true, onActiveChange = {

    }, placeholder = {
        Text(text = "search")
    }, leadingIcon = {
        Icon(Icons.Filled.ArrowBack, contentDescription = "back", modifier = Modifier.clickable {
            back()
        })
    }, trailingIcon = {
        Icon(Icons.Filled.Clear, contentDescription = "clear", modifier = Modifier.clickable {
            viewModel.keyword.value = ""
        })
    }) {
        var selected by remember {
            mutableStateOf(0)
        }
        val pagerState = rememberPagerState()

        val coroutineScope = rememberCoroutineScope()
        TabRow(selectedTabIndex = selected) {
            val l = listOf("video", "bnagumi", "up")
            l.forEachIndexed { i, e ->
                Tab(selected = selected ==i, onClick = {
                    selected = i
                    coroutineScope.launch {
                        pagerState.scrollToPage(i)
                    }
                }) {
                    Text(text = e)
                }
            }
        }
        HorizontalPager(pageCount = 3, state = pagerState) {
            when (it) {
                0 -> {
                    val pagingItems = viewModel.videoResult.collectAsLazyPagingItems()
                    StateView(pagingItems) {
                        LazyColumn {
                            items(
                                count = pagingItems.itemCount,
                                key = pagingItems.itemKey(),
                                contentType = pagingItems.itemContentType(
                                )
                            ) { index ->
                                val item = pagingItems[index]
                                VideoItem(
                                    item?.cover,
                                    item?.title.orEmpty(),
                                    item?.author.orEmpty()
                                ) {
                                    current.playVideo(item?.mid)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val pagingItems =
                        viewModel.bangumiResult.collectAsLazyPagingItems()
                    StateView(pagingItems) {
                        LazyColumn {
                            items(
                                count = pagingItems.itemCount,
                                key = pagingItems.itemKey(),
                                contentType = pagingItems.itemContentType(
                                )
                            ) { index ->
                                val item = pagingItems[index]
                                VideoItem(
                                    item?.cover,
                                    item?.title.orEmpty(),
                                    item?.cat_desc.orEmpty()
                                ) {
//                                    current.playVideo(item?.param)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val pagingItems = viewModel.upResult.collectAsLazyPagingItems()
                    StateView(pagingItems) {
                        LazyColumn {
                            items(
                                count = pagingItems.itemCount,
                                key = pagingItems.itemKey(),
                                contentType = pagingItems.itemContentType(
                                )
                            ) { index ->
                                val item = pagingItems[index]
                                UpItem(item)
                            }
                        }
                    }
                }
            }

        }

    }

}

@Composable
fun UpItem(item: SearchUpperInfo?) {
    Text(text = item?.title.orEmpty())
}

class VideoSearchViewModel : ViewModel() {
    val keyword = MutableStateFlow("原神")

    @OptIn(ExperimentalCoroutinesApi::class)
    val videoResult = keyword.flatMapLatest {
        Pager(
            PagingConfig(pageSize = 20)
        ) {
            SearchSource(it)
        }.flow
            .cachedIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val bangumiResult = keyword.flatMapLatest {
        Pager(
            PagingConfig(pageSize = 20)
        ) {
            SearchBangumiSource(it)
        }.flow
            .cachedIn(viewModelScope)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val upResult = keyword.flatMapLatest {
        Pager(
            PagingConfig(pageSize = 20)
        ) {
            SearchUpSource(it)
        }.flow
            .cachedIn(viewModelScope)
    }
}

class SearchSource(private val keyword: String) : PagingSource<Int, SearchVideoInfo>() {
    override fun getRefreshKey(state: PagingState<Int, SearchVideoInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchVideoInfo> {
        val pageNum = params.key ?: 1
        if (keyword == "") {
            return LoadResult.Error(Exception("不能为空"))
        }
        return try {
            val res = BiliApiService.searchApi
                .searchArchive(
                    keyword = keyword,
                    order = "default",
                    duration = 0,
                    rid = 0,
                    pageNum = 1,
                    pageSize = params.loadSize
                )
                .awaitCall()
                .gson<ResultInfo<SearchResultInfo<SearchArchiveInfo>>>()
            if (res.code == 0) {
                val archive = res.data.items.archive.orEmpty()
                LoadResult.Page(archive, null, if (archive.isEmpty()) null else pageNum + 1)
            } else {
                LoadResult.Error(Exception(res.error()))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    }

}

class SearchUpSource(private val keyword: String) : PagingSource<Int, SearchUpperInfo>() {
    override fun getRefreshKey(state: PagingState<Int, SearchUpperInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchUpperInfo> {
        val pageNum = params.key ?: 1
        if (keyword == "") {
            return LoadResult.Error(Exception("不能为空"))
        }
        return try {
            val res = BiliApiService.searchApi
                .searchUpper(
                    keyword = keyword,
                    pageNum = 1,
                    pageSize = params.loadSize
                )
                .awaitCall()
                .gson<ResultInfo<SearchListInfo<SearchUpperInfo>>>()
            if (res.code == 0) {
                val archive = res.data.items.orEmpty()
                LoadResult.Page(archive, null, if (archive.isEmpty()) null else pageNum + 1)
            } else {
                LoadResult.Error(Exception(res.error()))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    }

}

class SearchBangumiSource(private val keyword: String) : PagingSource<Int, SearchBangumiInfo>() {
    override fun getRefreshKey(state: PagingState<Int, SearchBangumiInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchBangumiInfo> {
        val pageNum = params.key ?: 1
        if (keyword == "") {
            return LoadResult.Error(Exception("不能为空"))
        }
        return try {
            val res = BiliApiService.searchApi
                .searchBangumi(
                    keyword = keyword,
                    pageNum = 1,
                    pageSize = params.loadSize
                )
                .awaitCall()
                .gson<ResultInfo<SearchListInfo<SearchBangumiInfo>>>()
            if (res.code == 0) {
                val archive = res.data.items.orEmpty()
                LoadResult.Page(archive, null, if (archive.isEmpty()) null else pageNum + 1)
            } else {
                LoadResult.Error(Exception(res.error()))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    }

}

fun <T> ResultInfo<T>.error(): Exception {
    return java.lang.Exception("$code $message")
}
