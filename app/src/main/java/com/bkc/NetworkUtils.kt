package com.bkc

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager

object NetworkUtils {
    fun isOnline(context: Context): Boolean {
        // network manager
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        @SuppressLint("MissingPermission") val netInfo = connectivityManager.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }
}