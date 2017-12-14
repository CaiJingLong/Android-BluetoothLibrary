package tk.kikt.bluetoothmanager

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import tk.kikt.bluetoothmanager.handler.BluetoothHandler
import tk.kikt.bluetoothmanager.handler.BluetoothType
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@SuppressLint("StaticFieldLeak")
/**
 * Created by cai on 2017/12/14.
 */
object BluetoothConnectManager {

    lateinit var application: Application

    var adapter: BluetoothAdapter? = null

    fun init(application: Application) {
        this.application = application
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            "不支持蓝牙设备"
            return
        }
        this.adapter = adapter
    }

    private val locker: Lock = ReentrantLock()

    private val handlerMap = HashMap<BluetoothType, BluetoothHandler>()

    fun registerHandler(type: BluetoothType, handler: BluetoothHandler) {
        locker.withLock {
            handlerMap[type] = handler
        }
    }

    fun unregisterHandler(type: BluetoothType) {
        locker.withLock {
            handlerMap.remove(type)
        }
    }

}