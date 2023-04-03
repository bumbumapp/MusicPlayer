package org.bumbumapps.musicplayer.util

import android.content.Context

class MyPreference (context: Context){
    private var SHARD_PREFENCE="Shared"

    val prefence=context.getSharedPreferences(SHARD_PREFENCE,Context.MODE_PRIVATE)

    fun getBoolen():Boolean{
        return prefence.getBoolean("audio",true)
    }
    fun setBoolen(bl:Boolean) {
        val editor = prefence.edit()
        editor.putBoolean("audio", bl)
        editor.apply()
    }
}