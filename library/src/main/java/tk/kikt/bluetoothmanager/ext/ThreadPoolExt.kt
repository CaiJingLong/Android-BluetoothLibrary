package tk.kikt.bluetoothmanager.ext

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