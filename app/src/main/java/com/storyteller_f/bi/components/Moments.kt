package com.storyteller_f.bi.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import bilibili.app.dynamic.v2.DynamicCommonOuterClass
import bilibili.app.dynamic.v2.DynamicGrpc
import bilibili.app.dynamic.v2.DynamicOuterClass
import bilibili.app.dynamic.v2.ModuleOuterClass
import com.a10miaomiao.bilimiao.comm.entity.comm.PaginationInfo
import com.a10miaomiao.bilimiao.comm.network.request
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.storyteller_f.bi.LoadingState
import kotlinx.coroutines.launch

@Composable
fun MomentsPage() {
    val viewModel = viewModel<MomentsViewModel>()
    val observeAsState by viewModel.state.observeAsState()
    val list = viewModel.list
    when (val state = observeAsState) {
        is LoadingState.Loading -> Text(text = "loading")
        is LoadingState.Error -> Text(text = state.e.localizedMessage ?: "")
        is LoadingState.Done -> LazyColumn {
            items(list.data.size) {
                val cursorItem = list.data[it]
                MomentItem(cursorItem, editMode = false)
            }
        }

        else -> Text(text = "impossible")
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
                    )
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
    val videoSize = Modifier
        .width(80.dp)
        .height(45.dp)
    Column {
        Row {
            if (editMode)
                Box(modifier = authorSize)
            else
                GlideImage(model = dataInfo.face, contentDescription = "", modifier = authorSize)
            Column {
                Text(text = dataInfo.name)
                Text(text = dataInfo.labelText)
            }
        }
        val dynamicContent = dataInfo.dynamicContent
        VideoItem(dynamicContent.pic, dynamicContent.title, dynamicContent.remark.orEmpty())
        Row {
            if (editMode)
                Box(modifier = videoSize)
            else
                GlideImage(
                    model = UrlUtil.autoHttps(dynamicContent.pic) + "@672w_378h_1c_",
                    contentDescription = "",
                    modifier = videoSize
                )
            Column {
                Text(text = dynamicContent.title)
                Text(text = dynamicContent.remark.orEmpty())
            }
        }
        Row {
            Text(text = "up ${dataInfo.like}")
            Text(text = "comment ${dataInfo.reply}", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

class MomentsViewModel() : ViewModel() {
    val state = MutableLiveData<LoadingState>()
    var list = PaginationInfo<DataInfo>()

    init {
        load()
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

    private fun load() {
        state.loading()
        viewModelScope.launch {
            try {
                val req = DynamicOuterClass.DynVideoReq.newBuilder()
                    .setRefreshType(DynamicCommonOuterClass.Refresh.refresh_new)
                    .setLocalTime(8)
                    .setOffset("")
                    .setUpdateBaseline("")
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
                        )
                    }
                    list.data.addAll(itemsList)
                }
                state.loaded()
            } catch (e: Exception) {
                state.error(e)
            }
        }
    }
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
)

data class DynamicContentInfo(
    val id: String,
    val title: String = "",
    val pic: String = "",
    val remark: String? = null,
)