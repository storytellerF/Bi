package com.storyteller_f.bi

import android.app.Application
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp

class App: Application() {

    companion object {
        const val APP_NAME = "bilimiao"
        lateinit var commApp: BilimiaoCommApp
    }

    init {
        commApp = BilimiaoCommApp(this)
    }

    override fun onCreate() {
        super.onCreate()
//        ThemeDelegate.setNightMode(this)
//        Mojito.initialize(
//            GlideImageLoader.with(this),
//            SketchImageLoadFactory()
//        )
        commApp.onCreate()
        readUserInfo()
    }
}