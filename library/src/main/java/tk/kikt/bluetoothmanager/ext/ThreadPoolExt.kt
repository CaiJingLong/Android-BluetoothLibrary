package tk.kikt.bluetoothmanager.ext

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService

/**
 * Created by cai on 2017/12/14.
 */
fun ExecutorService.checkShutdown(onShutDown: (() -> Unit)? = null) {
    if (!this.isShutdown) {
        this.shutdownNow()
        onShutDown?.invoke()
    }
}

val handler = Handler(Looper.getMainLooper())

fun runDelay(delay: Long, action: () -> Unit) {
    if (delay == 0L) {
        handler.post(action)
        return
    }
    handler.postDelayed(action, delay)
}