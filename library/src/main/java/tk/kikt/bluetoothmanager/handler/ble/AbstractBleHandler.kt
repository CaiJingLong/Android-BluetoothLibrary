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
import tk.kikt.bluetoothmanager.ext.trimAndIgnoreCaseEquals
import tk.kikt.bluetoothmanager.ext.uiThread
import tk.kikt.bluetoothmanager.log
import java.util.*

/**
 * Created by cai on 2017/12/14.
 */
abstract class AbstractBleHandler : Logger {

    override fun isLog() = BluetoothConnectManager.isLog()

    private val application: Context
        get() = BluetoothConnectManager.application

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun connect(name: String) {
        checkAdapter { adapter ->
            BluetoothHelper.withOpen {
                val connector: BleScanner =
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            API21Impl
                        } else {
                            NewImpl
                        }

                callbackExec { it.onBeginStartScanDevice() }

                connector.scan(adapter) { success, list ->
                    if (success.not()) {
                        callbackExec { it.onScanEnd(null) }
                        return@scan
                    }
                    for (bluetoothDevice in list) {
                        if (bluetoothDevice.name trimAndIgnoreCaseEquals name) {
                            callbackExec { it.onScanEnd(bluetoothDevice) }
                            scanService(bluetoothDevice)
                            return@scan
                        }
                    }
                    callbackExec { it.onScanEnd(null) }
                }
            }
        }
    }

    abstract fun serviceUUID(): UUID
    abstract fun characteristicUUID(): UUID


    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun scanService(device: BluetoothDevice) {
        device.connectGatt(application, true, gattCallback)
    }

    val gattCallback = object : BluetoothGattCallback() {

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) {
                gatt?.disconnect()
                return
            }
            gatt.services?.forEach { service ->
                if (service.uuid == serviceUUID()) {
                    val characteristic = service.getCharacteristic(characteristicUUID())
                    handleCharacteristic(gatt, service, characteristic)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            onNotifyChange(gatt, characteristic)
        }

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            log("status = $status  newState:$newState")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                callbackExec { it.onConnect(false) }
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    callbackExec { it.onConnect(true) }
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    callbackExec { it.onDisconnect() }
                }
            }
        }

    }

    protected abstract fun handleCharacteristic(gatt: BluetoothGatt?, service: BluetoothGattService, characteristic: BluetoothGattCharacteristic)

    protected abstract fun onNotifyChange(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?)

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

    val callbacks = ArrayList<BleStateCallback>()

    fun addCallback(callback: BleStateCallback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: BleStateCallback) {
        callbacks.remove(callback)
    }

    fun callbackExec(cb: (callback: BleStateCallback) -> Unit) {
        callbacks.forEach {
            uiThread {
                cb(it)
            }
        }
    }

    interface BleStateCallback {

        /**
         * 扫描开始
         */
        fun onBeginStartScanDevice()

        /**
         * 扫描结束
         * @param device 为null则未找到,否则则认为找到设备
         */
        fun onScanEnd(device: BluetoothDevice?)


        /**
         * 连接设备
         * @param success true为成功 false为不成功
         */
        fun onConnect(success: Boolean)

        /**
         * 设备连接断开(不区分原因)
         */
        fun onDisconnect()

        /**
         * 设备准备完毕,可以开始业务逻辑
         */
        fun onPrepared(device: BluetoothDevice?)

    }
}
