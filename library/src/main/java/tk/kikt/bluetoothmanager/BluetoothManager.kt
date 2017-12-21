package tk.kikt.bluetoothmanager

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import tk.kikt.bluetoothmanager.handler.BluetoothHandler
import tk.kikt.bluetoothmanager.handler.BluetoothType
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@SuppressLint("StaticFieldLeak")
/**
 * Created by cai on 2017/12/14.
 */
object BluetoothConnectManager : Logger {

    override fun isLog(): Boolean {
        return true
    }

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

    fun registerHandler(handler: BluetoothHandler) {
        locker.withLock {
            handlerMap[handler.type()] = handler
        }
        notifyConnectChange()
    }

    fun unregisterHandler(type: BluetoothType) {
        locker.withLock {
            handlerMap.remove(type)
        }
        notifyConnectChange()
    }

    fun getHandlerWithType(type: BluetoothType): BluetoothHandler? {
        locker.withLock {
            return handlerMap[type]
        }
    }

    var currentDevice: BluetoothDevice? = null

    var currentConnectHandler: BluetoothHandler? = null

    fun notifyConnectChange() {
        locker.withLock {
            for (handler in handlerMap.values) {
                val device = handler.getConnectDevice()
                if (device != null) {
                    currentDevice = device
                    currentConnectHandler = handler
                    onConnectingDeviceListenerList.forEach {
                        it.onConnectDeviceChange(newDevice = device)
                    }
                    return@withLock
                }
            }
            currentDevice = null
            currentConnectHandler = null
            onConnectingDeviceListenerList.forEach {
                it.onConnectDeviceChange(newDevice = null)
            }
        }
    }

    fun disableBluetooth() {
        adapter?.disable()
    }

    private val deviceCbLocker = ReentrantLock()

    interface OnConnectingDeviceListener {
        fun onConnectDeviceChange(newDevice: BluetoothDevice?)
    }

    private val onConnectingDeviceListenerList = arrayListOf<OnConnectingDeviceListener>()

    fun addOnConnectingDeviceListener(listener: OnConnectingDeviceListener) {
        deviceCbLocker.withLock {
            onConnectingDeviceListenerList.add(listener)
        }
    }

    fun removeOnConnectingDeviceListener(listener: OnConnectingDeviceListener) {
        deviceCbLocker.withLock {
            onConnectingDeviceListenerList.remove(listener)
        }
    }
}