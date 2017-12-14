package tk.kikt.bluetoothmanager.handler.ble

import android.annotation.TargetApi
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresApi
import android.widget.Toast
import tk.kikt.bluetoothmanager.BluetoothConnectManager
import tk.kikt.bluetoothmanager.BluetoothHelper
import tk.kikt.bluetoothmanager.Logger
import tk.kikt.bluetoothmanager.log

/**
 * Created by cai on 2017/12/14.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
abstract class AbstractBleHandler : Logger {

    override fun isLog(): Boolean {
        return true
    }

    private var application: Context = BluetoothConnectManager.application

    fun connect(name: String) {
        checkAdapter { adapter ->
            BluetoothHelper.withOpen {
                val connector: BleScanner =
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            API21Impl
                        } else {
                            NewImpl
                        }

                connector.scan(adapter) { success, list ->
                    log("$success ")
                    list.forEach {
                        if (it.name == name) {
                            scanService(it)
                        }
                    }
                }
            }
        }
    }

    fun scanService(device: BluetoothDevice) {
        device.connectGatt(application, true, gattCallback)
    }

    val gattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {

                }
            }
        }
    }

    private inline fun checkAdapter(crossinline action: (adapter: BluetoothAdapter) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            toast("您的系统版本过低,无法使用该设备,请升级到4.4或更高版本继续尝试")
            return
        }
        val adapter = BluetoothConnectManager.adapter

        if (adapter == null) {
            toast("没有合适的蓝牙设备")
            return
        }
        action(adapter)
    }

    private fun toast(msg: String) {
        Toast.makeText(application, msg, Toast.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    object API21Impl : BleScanner {
        private val deviceList = ArrayList<BluetoothDevice>()

        override fun scan(adapter: BluetoothAdapter, action: (success: Boolean, list: List<BluetoothDevice>) -> Unit) {
            Callback.cb = action
            adapter.startLeScan(Callback)
            MyHandler.postDelayed({
                MyHandler.removeCallbacksAndMessages(null)
                Callback.cb(true, deviceList.toList())
                adapter.stopLeScan(Callback)
            }, 5000)
        }

        object Callback : BluetoothAdapter.LeScanCallback {
            var cb = { success: Boolean, list: List<BluetoothDevice> -> }
            override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
                if (device != null && deviceList.contains(device).not()) {
                    deviceList.add(device)
                }
            }
        }

        private object MyHandler : Handler()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    object NewImpl : BleScanner, Logger {
        override fun isLog(): Boolean {
            return true
        }

        private val deviceList = ArrayList<BluetoothDevice>()

        override fun scan(adapter: BluetoothAdapter, action: (success: Boolean, list: List<BluetoothDevice>) -> Unit) {
            Callback.cb = action
            adapter.bluetoothLeScanner.startScan(Callback)
            val function = {
                adapter.bluetoothLeScanner.stopScan(Callback)
                MyHandler.removeCallbacksAndMessages(null)
                Callback.cb(true, deviceList.toList())
            }
            MyHandler.postDelayed(function, 5000)
        }

        object Callback : ScanCallback() {
            var cb = { success: Boolean, list: List<BluetoothDevice> -> }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                MyHandler.removeCallbacksAndMessages(null)
                Callback.cb(false, listOf())
            }

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                val device = result?.device
                if (device != null && deviceList.contains(device).not()) {
                    deviceList.add(device)
                }
            }
        }

        private object MyHandler : Handler()
    }

    interface BleScanner {
        fun scan(adapter: BluetoothAdapter, action: (success: Boolean, list: List<BluetoothDevice>) -> Unit)
    }

    interface BleCallback {

    }
}
