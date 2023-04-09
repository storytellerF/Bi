package com.storyteller_f.bi.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import bilibili.app.dynamic.v2.Desc
import bilibili.app.dynamic.v2.DynamicCommonOuterClass
import bilibili.app.dynamic.v2.DynamicGrpc
import bilibili.app.dynamic.v2.DynamicOuterClass
import bilibili.app.dynamic.v2.ModuleOuterClass
import com.a10miaomiao.bilimiao.comm.network.request
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.OneCenter

@Composable
fun MomentsPage() {
    val viewModel = viewModel<MomentsViewModel>()
    val lazyPagingItems = viewModel.flow.collectAsLazyPagingItems()
    when (val state = lazyPagingItems.loadState.refresh) {
        is LoadState.Error -> OneCenter {
            Text(text = state.error.localizedMessage ?: "")
        }
        else -> LazyColumn {
            if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                item {
                    Text(
                        text = "Waiting for items to load from the backend",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
            items(lazyPagingItems) {
                MomentItem(it ?: MomentsPreviewProvider().values.first(), editMode = false)
            }
            if (lazyPagingItems.loadState.append == LoadState.Loading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

class MomentsPreviewProvider : PreviewParameterProvider<DataInfo> {
    override val values: Sequence<DataInfo>
        get() = sequence {
            yield(
                DataInfo(
                    "",
                    "test",
                    "https://i0.hdslb.com/bfs/face/member/noface.jpg",
                    "labelText",
                    90,
                    90,
                    1,
                    DynamicContentInfo(
                        "i",
                        "title",
                        pic = "https://i0.hdslb.com/bfs/face/member/noface.jpg"
                    ),
                    Desc.ModuleDesc.getDefaultInstance()
                )
            )
        }

}

@Preview
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MomentItem(
    @PreviewParameter(MomentsPreviewProvider::class) dataInfo: DataInfo,
    editMode: Boolean = true
) {
    val authorSize = Modifier.size(40.dp)
    Column {
        Row(modifier = Modifier.padding(8.dp)) {
            if (editMode)
                Box(modifier = authorSize)
            else
                GlideImage(model = dataInfo.face, contentDescription = "", modifier = authorSize)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = dataInfo.name)
                Text(text = dataInfo.labelText)
            }
        }
        val dynamicContent = dataInfo.dynamicContent
        VideoItem(dynamicContent.pic, dynamicContent.title, dynamicContent.remark.orEmpty())
        Row {
            Text(text = "up ${dataInfo.like}")
            Text(text = "comment ${dataInfo.reply}", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

class MomentsViewModel : ViewModel() {
    val flow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        MomentsPagingSource()
    }.flow
        .cachedIn(viewModelScope)
}

data class DataInfo(
    val mid: String,
    val name: String,
    val face: String,
    val labelText: String,
    val like: Long,
    val reply: Long,
    val dynamicType: Int,
    val dynamicContent: DynamicContentInfo,
    val descMode: Desc.ModuleDesc?,
)

data class DynamicContentInfo(
    val id: String,
    val title: String = "",
    val pic: String = "",
    val remark: String? = null,
)

class MomentsPagingSource : PagingSource<Pair<String, String>, DataInfo>() {
    override suspend fun load(
        params: LoadParams<Pair<String, String>>
    ): LoadResult<Pair<String, String>, DataInfo> {
        try {
            val (offset, baseline) = params.key ?: ("" to "")
            val type = if (offset.isBlank()) {
                DynamicCommonOuterClass.Refresh.refresh_new
            } else {
                DynamicCommonOuterClass.Refresh.refresh_history
            }
            val req = DynamicOuterClass.DynVideoReq.newBuilder()
                .setRefreshType(type)
                .setLocalTime(8)
                .setOffset(offset)
                .setUpdateBaseline(baseline)
                .build()

            val result = DynamicGrpc.getDynVideoMethod()
                .request(req)
                .awaitCall()
            if (result.hasDynamicList()) {
                val dynamicListData = result.dynamicList
                val itemsList = dynamicListData.listList.filter { item ->
                    item.cardType != DynamicCommonOuterClass.DynamicType.dyn_none
                            && item.cardType != DynamicCommonOuterClass.DynamicType.ad
                }.map { item ->
                    val modules = item.modulesList
                    val userModule = modules.first { it.hasModuleAuthor() }.moduleAuthor
                    val descModule = modules.find { it.hasModuleDesc() }?.moduleDesc
                    val dynamicModule = modules.first { it.hasModuleDynamic() }.moduleDynamic
                    val statModule = modules.first { it.hasModuleStat() }.moduleStat
                    DataInfo(
                        mid = userModule.author.mid.toString(),
                        name = userModule.author.name,
                        face = userModule.author.face,
                        labelText = userModule.ptimeLabelText,
                        dynamicType = dynamicModule.typeValue,
                        like = statModule.like,
                        reply = statModule.reply,
                        dynamicContent = getDynamicContent(dynamicModule),
                        descMode = descModule,
                    )
                }
                return LoadResult.Page(
                    data = itemsList,
                    prevKey = null, // Only paging forward.
                    nextKey = dynamicListData.historyOffset to dynamicListData.updateBaseline
                )
            } else {
                return LoadResult.Page(
                    data = listOf(),
                    prevKey = null, // Only paging forward.
                    nextKey = null
                )
            }

        } catch (e: Exception) {
            // Handle errors in this block and return LoadResult.Error if it is an
            // expected error (such as a network failure).
            return LoadResult.Error(e)
        }
    }


    private fun getDynamicContent(dynamicModule: ModuleOuterClass.ModuleDynamic): DynamicContentInfo {
        return when (dynamicModule.type) {
            ModuleOuterClass.ModuleDynamicType.mdl_dyn_archive -> {
                val dynArchive = dynamicModule.dynArchive
                DynamicContentInfo(
                    id = dynArchive.avid.toString(),
                    title = dynArchive.title,
                    pic = dynArchive.cover,
                    remark = dynArchive.coverLeftText2 + "    " + dynArchive.coverLeftText3,
                )
            }

            ModuleOuterClass.ModuleDynamicType.mdl_dyn_pgc -> {
                val dynPgc = dynamicModule.dynPgc
                DynamicContentInfo(
                    id = dynPgc.seasonId.toString(),
                    title = dynPgc.title,
                    pic = dynPgc.cover,
                    remark = dynPgc.coverLeftText2 + "    " + dynPgc.coverLeftText3,
                )
            }

            else -> DynamicContentInfo("")
        }
    }

    override fun getRefreshKey(state: PagingState<Pair<String, String>, DataInfo>): Pair<String, String>? {
        return null
    }

    companion object {
    }
}