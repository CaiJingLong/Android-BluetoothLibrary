package tk.kikt.bluetoothmanager

import android.util.Log

/**
 * Created by cai on 2017/12/14.
 */
interface Logger {
    fun isLog(): Boolean

}

fun Logger.log(msg: Any) {
    if (isLog()) {
        Log.i(this.javaClass.simpleName, msg.toString())
    }
}

fun Logger.log(msg: Any, throwable: Throwable) {
    if (isLog()) {
        Log.i(this.javaClass.simpleName, msg.toString(), throwable)
    }
}