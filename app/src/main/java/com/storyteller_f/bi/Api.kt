import bilibili.app.interfaces.v1.HistoryGrpc
import bilibili.app.interfaces.v1.HistoryOuterClass
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

}
