package com.storyteller_f.bi.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo2
import com.a10miaomiao.bilimiao.comm.entity.search.SearchArchiveInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchBangumiInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchListInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchResultInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchUpperInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchVideoInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.StandBy
import com.storyteller_f.bi.StateView
import com.storyteller_f.bi.defaultFactory
import com.storyteller_f.bi.playVideo
import com.storyteller_f.bi.ui.theme.BiTheme
import com.storyteller_f.bi.unstable.logout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@Composable
fun SearchPage(
    modifier: Modifier = Modifier,
    userInfo: UserInfo? = null,
    dockMode: Boolean = false,
    noMiddleState: Boolean = false,
    login: () -> Unit = {},
    back: () -> Unit = {}
) {
    val viewModel = viewModel<VideoSearchViewModel>(factory = defaultFactory)
    var activated by remember {
        mutableStateOf(false)
    }
    var input by remember {
        mutableStateOf(viewModel.keyword.value)
    }

    val trailingIcon = @Composable {
        Row(modifier = Modifier.padding(end = 8.dp)) {
            Icon(Icons.Filled.Clear, contentDescription = "clear", modifier = Modifier.clickable {
                viewModel.keyword.value = ""
            })
            if (!activated && userInfo != null) HomeAvatar(userInfo, login)
        }
    }
    val leadingIcon = @Composable {
        Icon(Icons.Filled.ArrowBack, contentDescription = "back", modifier = Modifier.clickable {
            when {
                noMiddleState -> back()
                activated -> activated = false
                else -> back()
            }
        })
    }
    val onActiveChange: (Boolean) -> Unit = {
        activated = it
    }
    val onSearch: (String) -> Unit = {
        viewModel.keyword.value = it
        input = it
    }
    val onQueryChange: (String) -> Unit = {
        input = it
    }
    val placeholder = @Composable {
        Text(text = "search")
    }
    val content: @Composable (ColumnScope.() -> Unit) = {
        SearchContent(viewModel)
    }
    CombinedSearchBar(
        dockMode,
        input,
        onQueryChange,
        onSearch,
        activated,
        onActiveChange,
        placeholder,
        leadingIcon,
        trailingIcon,
        modifier,
        content
    )
    LaunchedEffect(key1 = noMiddleState) {
        if (noMiddleState) {
            activated = true
        }
    }
    BackHandler(enabled = noMiddleState) {
        back()
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CombinedSearchBar(
    dockMode: Boolean,
    input: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    activated: Boolean,
    onActiveChange: (Boolean) -> Unit,
    placeholder: @Composable () -> Unit,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier,
    content: @Composable (ColumnScope.() -> Unit)
) {
    if (dockMode) {
        DockedSearchBar(
            query = input,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = activated,
            onActiveChange = onActiveChange,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = modifier, content = content
        )
    } else {
        SearchBar(
            query = input,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = activated,
            onActiveChange = onActiveChange,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = modifier, content = content
        )
    }
}

@Composable
@OptIn(ExperimentalGlideComposeApi::class,
    ExperimentalMaterial3Api::class
)
private fun HomeAvatar(
    userInfo: UserInfo,
    login: () -> Unit = {},
) {
    val coverSize = Modifier
        .padding(start = 8.dp)
        .size(24.dp)
    var showPopup by remember {
        mutableStateOf(false)
    }
    StandBy(coverSize) {
        GlideImage(
            model = UrlUtil.autoHttps(userInfo.face),
            contentDescription = "avatar",
            modifier = coverSize.clickable {
                showPopup = true
            }
        )
    }
    if (showPopup) {
        AlertDialog(onDismissRequest = { showPopup = false }, properties = DialogProperties(decorFitsSystemWindows = false)) {
            Surface {
                AvatarContent(userInfo, login)
            }
        }
    }
}

@Preview
@Composable
private fun AvatarContent(userInfo: UserInfo? = null, login: () -> Unit = {}) {
    Column {
        val context = LocalContext.current
        Spacer(Modifier.height(12.dp))
        UserBanner(u = userInfo, login)
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(label = { Text(text = "Setting") }, icon = {
            Icon(Icons.Filled.Settings, contentDescription = "setting")
        }, selected = false, onClick = {
            Toast.makeText(
                context,
                "not implementation",
                Toast.LENGTH_SHORT
            )
                .show()
        })
        NavigationDrawerItem(label = { Text(text = "Logout") }, icon = {
            Icon(Icons.Filled.Close, contentDescription = "logout")
        }, selected = false, onClick = {
            context.logout()
        })
    }
}

@Preview
@Composable
private fun PreviewSearchPage() {
    BiTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            SearchPage(modifier = Modifier) {

            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SearchContent(
    viewModel: VideoSearchViewModel,
) {
    val current = LocalContext.current

    var selected by remember {
        mutableIntStateOf(0)
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        3
    }

    val coroutineScope = rememberCoroutineScope()
    TabRow(selectedTabIndex = selected) {
        val l = listOf("video", "bangumi", "up")
        l.forEachIndexed { i, e ->
            Tab(selected = selected == i, onClick = {
                selected = i
                coroutineScope.launch {
                    pagerState.scrollToPage(i)
                }
            }) {
                Text(text = e, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
    //                                    current.playVideo(item?.param)
    HorizontalPager(state = pagerState) {
        SearchPageBuilder(it, viewModel, current)
    }

}

@Composable
private fun SearchPageBuilder(
    it: Int,
    viewModel: VideoSearchViewModel,
    current: Context
) {
    when (it) {
        0 -> {
            List(viewModel.videoResult.collectAsLazyPagingItems()) { item ->
                VideoItem(
                    item?.cover,
                    item?.title.orEmpty(),
                    item?.author.orEmpty()
                ) {
                    current.playVideo(item?.param, item?.param, "archive")
                }
            }
        }

        1 -> {
            val pagingItems =
                viewModel.bangumiResult.collectAsLazyPagingItems()
            List(lazyPagingItems = pagingItems) { item ->
                VideoItem(
                    item?.cover,
                    item?.title.orEmpty(),
                    item?.cat_desc.orEmpty()
                ) {
//                                    current.playVideo(item?.param)
                }
            }
        }

        2 -> {
            val pagingItems = viewModel.upResult.collectAsLazyPagingItems()
            List(lazyPagingItems = pagingItems) { item ->
                UpItem(item)
            }
        }
    }
}

@Composable
private fun <T : Any> List(
    lazyPagingItems: LazyPagingItems<T>,
    content: @Composable (T?) -> Unit
) {
    StateView(lazyPagingItems) {
        LazyColumn {
            topRefreshing(lazyPagingItems)
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey(),
                contentType = lazyPagingItems.itemContentType()
            ) { index ->
                val item = lazyPagingItems[index]
                content(item)
            }
            bottomAppending(lazyPagingItems)
        }
    }
}

class UpItemPreviewProvider : PreviewParameterProvider<SearchUpperInfo?> {
    override val values: Sequence<SearchUpperInfo?>
        get() = sequence {
            yield(SearchUpperInfo("test", "", "", "", "", 1, "sign", 1, 1, 1))
        }

}

@OptIn(ExperimentalGlideComposeApi::class)
@Preview
@Composable
fun UpItem(@PreviewParameter(UpItemPreviewProvider::class) item: SearchUpperInfo?) {
    Row(modifier = Modifier.padding(8.dp)) {
        val modifier = Modifier.size(40.dp)
        StandBy(modifier) {
            val cover = item?.cover
            GlideImage(
                model = if (cover != null) UrlUtil.autoHttps("$cover@200w_200h") else null,
                contentDescription = "cover", modifier = modifier
            )
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = item?.title.orEmpty())
            Text(text = item?.sign.orEmpty())
        }
    }
}

class VideoSearchViewModel : ViewModel() {
    val keyword = MutableStateFlow("")

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

fun <T, K : Any, V : Any> ResultInfo<T>.loadResultError(): PagingSource.LoadResult<K, V> {
    return PagingSource.LoadResult.Error(Exception("$code $message"))
}

fun <T> ResultInfo2<T>.error(): Exception {
    return Exception("$code $message")
}

