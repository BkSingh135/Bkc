package com.bkc.zoom

import android.util.Log
import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Utility class that can log traces and info.
 */
class ZoomLogger private constructor(private val mTag: String) {
    @IntDef(LEVEL_VERBOSE, LEVEL_WARNING, LEVEL_ERROR)
    @Retention(RetentionPolicy.SOURCE)
    internal annotation class LogLevel

    fun v(message: String?) {
        if (should(LEVEL_VERBOSE)) {
            Log.v(mTag, message!!)
            lastMessage = message
            lastTag = mTag
        }
    }

    fun i(message: String?) {
        if (should(LEVEL_INFO)) {
            Log.i(mTag, message!!)
            lastMessage = message
            lastTag = mTag
        }
    }

    fun w(message: String?) {
        if (should(LEVEL_WARNING)) {
            Log.w(mTag, message!!)
            lastMessage = message
            lastTag = mTag
        }
    }

    fun e(message: String?) {
        if (should(LEVEL_ERROR)) {
            Log.e(mTag, message!!)
            lastMessage = message
            lastTag = mTag
        }
    }

    private fun should(messageLevel: Int): Boolean {
        return level <= messageLevel
    }

    private fun string(messageLevel: Int, vararg ofData: Any): String {
        var message = ""
        if (should(messageLevel)) {
            for (o in ofData) {
                message += o.toString()
                message += " "
            }
        }
        return message.trim { it <= ' ' }
    }

    fun v(vararg data: Any?) {
        i(string(LEVEL_VERBOSE, data))
    }

    fun i(vararg data: Any?) {
        i(string(LEVEL_INFO, data))
    }

    fun w(vararg data: Any?) {
        w(string(LEVEL_WARNING, data))
    }

    fun e(vararg data: Any?) {
        e(string(LEVEL_ERROR, data))
    }

    companion object {
        const val LEVEL_VERBOSE = 0
        const val LEVEL_INFO = 1
        const val LEVEL_WARNING = 2
        const val LEVEL_ERROR = 3
        private var level = LEVEL_ERROR
        fun setLogLevel(logLevel: Int) {
            level = logLevel
        }

        var lastMessage: String? = null
        var lastTag: String? = null
        fun create(tag: String): ZoomLogger {
            return ZoomLogger(tag)
        }
    }
}

