package com.push.rotatetest

import android.app.Application
import android.content.Context

class MyApp : Application() {

    companion object{
        var appContext: Context? = null
    }


    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
    }
}