package com.storyteller_f.bi

import bilibili.app.interfaces.v1.HistoryGrpc
import bilibili.app.interfaces.v1.HistoryOuterClass
import bilibili.main.community.reply.v1.ReplyGrpc
import bilibili.main.community.reply.v1.ReplyOuterClass
import com.a10miaomiao.bilimiao.comm.network.request

object Api {
    suspend fun requestHistory(
        lastMax: Long,
        lastTp: Int
    ): HistoryOuterClass.CursorV2Reply? {
        val req = HistoryOuterClass.CursorV2Req.newBuilder().apply {
            business = "archive"
            cursor = HistoryOuterClass.Cursor.newBuilder().apply {
                if (lastMax != 0L) {
                    max = lastMax
                    maxTp = lastTp
                }
            }.build()
        }.build()
        return HistoryGrpc.getCursorV2Method()
            .request(req)
            .awaitCall()
    }

    suspend fun requestCommentList(
        id: String,
        sortOrder: Int = 3,
        cursor: ReplyOuterClass.CursorReply?
    ): ReplyOuterClass.MainListReply? {
        val req = ReplyOuterClass.MainListReq.newBuilder().apply {
            oid = id.toLong()
            type = 1
            rpid = 0
            this.cursor = ReplyOuterClass.CursorReq.newBuilder().apply {
                modeValue = sortOrder
                cursor?.let {
                    next = it.next
                }
            }.build()
        }.build()
        return ReplyGrpc.getMainListMethod().request(req)
            .awaitCall()
    }

}
