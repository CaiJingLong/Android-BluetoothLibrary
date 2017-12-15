package tk.kikt.bluetoothmanager.handler

import android.bluetooth.BluetoothDevice
import tk.kikt.bluetoothmanager.BluetoothConnectManager

/**
 * Created by cai on 2017/12/15.
 */
abstract class AbstractBluetoothHandler : BluetoothHandler {

    protected var currentDevice: BluetoothDevice? = null
        set(value) {
            field = value
            notifyDeviceChange()
        }

    override fun connectDevice() = currentDevice

    init {
        manage()
    }

    fun manage() {
        BluetoothConnectManager.registerHandler(this)
    }

    fun unmanageSelf() {
        BluetoothConnectManager.registerHandler(this)
    }

    protected fun notifyDeviceChange() {
        BluetoothConnectManager.notifyConnectChange()
    }
}