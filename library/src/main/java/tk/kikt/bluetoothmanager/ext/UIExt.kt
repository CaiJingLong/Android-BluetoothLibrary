package tk.kikt.bluetoothmanager.ext

import android.os.Handler
import android.os.Looper

/**
 * Created by cai on 2017/12/15.
 */
object UIHandler : Handler(Looper.getMainLooper())

inline fun uiThread(crossinline action: () -> Unit) {
    UIHandler.post {
        action()
    }
}