package tk.kikt.bluetoothmanager.handler

import android.bluetooth.BluetoothDevice

/**
 * Created by cai on 2017/12/14.
 */
interface BluetoothHandler {

    fun type(): BluetoothType

    fun getConnectDevice(): BluetoothDevice?

}