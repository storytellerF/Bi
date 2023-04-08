package com.storyteller_f.bi

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.google.gson.Gson
import java.io.File

var userInfo = MutableLiveData<UserInfo?>()
fun Context.saveUserInfo(userInfo: UserInfo?) {
    val file = File(filesDir.path + "/user.data")
    if (userInfo != null) {
        val jsonStr = Gson().toJson(userInfo)
        file.writeText(jsonStr)
    } else {
        file.delete()
    }
}

fun Context.readUserInfo() {
    try {
        val file = File(filesDir.path + "/user.data")
        if (file.exists()) {
            val jsonStr = file.readText()
            val localInfo = Gson().fromJson(jsonStr, UserInfo::class.java)
            userInfo.value = localInfo
        }
    } catch (e: Exception) {
        Log.e("UserInfoState", "readUserInfo: ", e)
    }
}
