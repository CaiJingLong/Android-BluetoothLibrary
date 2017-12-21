package tk.kikt.bluetoothmanager.handler

import android.bluetooth.BluetoothDevice
import tk.kikt.bluetoothmanager.BluetoothConnectManager
import tk.kikt.bluetoothmanager.ext.uiThread

/**
 * Created by cai on 2017/12/15.
 */
abstract class AbstractBluetoothHandler : BluetoothHandler {

    protected var currentDevice: BluetoothDevice? = null
        set(value) {
            field = value
            notifyDeviceChange()
        }

    override fun getConnectDevice() = currentDevice

    init {
        manage()
    }

    fun manage() {
        BluetoothConnectManager.registerHandler(this)
    }

    fun unmanageSelf() {
        BluetoothConnectManager.unregisterHandler(this.type())
    }

    protected fun notifyDeviceChange() {
        BluetoothConnectManager.notifyConnectChange()
    }

    private var onReadCallbackList = arrayListOf<OnReadCallback>()

    override fun addOnReadCallback(readCallback: OnReadCallback) {
        onReadCallbackList.add(readCallback)
    }

    override fun removeOnReadCallback(readCallback: OnReadCallback) {
        onReadCallbackList.remove(readCallback)
    }

    protected fun callReadByteArray(byteArray: ByteArray) {
        uiThread {
            for (onReadCallback in onReadCallbackList) {
                onReadCallback.onRead(byteArray)
            }
        }
    }
}


interface OnReadCallback {
    fun onRead(byteArray: ByteArray)
}