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
import tk.kikt.bluetoothmanager.handler.AbstractBluetoothHandler
import tk.kikt.bluetoothmanager.log
import java.util.*


/**
 * Created by cai on 2017/12/14.
 */
abstract class AbstractBleHandler : AbstractBluetoothHandler(), Logger {

    override fun isLog() = BluetoothConnectManager.isLog()

    private val application: Context
        get() = BluetoothConnectManager.application

    companion object {
        private val DURATION = 5000L
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun connect(name: String, time: Long = DURATION) {
        checkAdapter { adapter ->
            BluetoothHelper.withOpen {
                val connector: BleScanner =
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            API21Impl
                        } else {
                            NewImpl
                        }

                callbackExec { it.onBeginStartScanDevice() }

                connector.scanForName(adapter, name, time) { success, device ->
                    if (success.not() || device == null) {
                        callbackExec { it.onScanEnd(null) }
                    } else {
                        scanService(device)
                        callbackExec { it.onScanEnd(device) }
                    }
                }
            }
        }
    }

    /**
     * @return 要连接的serviceUUID
     */
    abstract fun serviceUUID(): UUID

    /**
     * @return 要连接的特征码UUID
     */
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
            val service = gatt.getService(serviceUUID())
            val characteristic = service.getCharacteristic(characteristicUUID())

            if (service == null) {
                gatt.disconnect()
                callbackExec { it.onNotFoundService(serviceUUID()) }
                return
            }

            if (characteristic == null) {
                gatt.disconnect()
                callbackExec { it.noNotFoundCharacteristic(characteristicUUID()) }
                return
            }

            val result = handleCharacteristic(gatt, service, characteristic)
            if (result) {
                callbackExec { it.onPrepared(gatt.device) }
            } else {
                gatt.disconnect()
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
                    currentDevice = gatt?.device
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    callbackExec { it.onDisconnect() }
                    currentDevice = null
                }
            }
        }

    }

    /**
     * 处理找到特征码后的操作
     * @return 返回false ,则直接断开设备连接,true则处理
     */
    protected abstract fun handleCharacteristic(gatt: BluetoothGatt?, service: BluetoothGattService, characteristic: BluetoothGattCharacteristic): Boolean

    /**
     * notify后接到通知后的回调
     */
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

        override fun scanForName(adapter: BluetoothAdapter, name: String, duration: Long, action: (success: Boolean, device: BluetoothDevice?) -> Unit) {
            fun fail() {
                action(false, null)
                adapter.stopLeScan(DeviceCallback)
                MyHandler.removeCallbacksAndMessages(null)
            }

            fun success(device: BluetoothDevice) {
                action(true, device)
                adapter.stopLeScan(DeviceCallback)
                MyHandler.removeCallbacksAndMessages(null)
            }

            MyHandler.postDelayed({ fail() }, duration)

            DeviceCallback.cb = {
                if (it != null && it.name trimAndIgnoreCaseEquals name) {
                    success(it)
                }
            }
            adapter.startLeScan(DeviceCallback)
        }

        override fun scanForList(adapter: BluetoothAdapter, duration: Long, action: (success: Boolean, list: List<BluetoothDevice>) -> Unit) {
            Callback.cb = action
            adapter.startLeScan(Callback)
            MyHandler.postDelayed({
                MyHandler.removeCallbacksAndMessages(null)
                Callback.cb(true, deviceList.toList())
                adapter.stopLeScan(Callback)
            }, duration)
        }

        object Callback : BluetoothAdapter.LeScanCallback {
            var cb = { success: Boolean, list: List<BluetoothDevice> -> }
            override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
                if (device != null && deviceList.contains(device).not()) {
                    deviceList.add(device)
                }
            }
        }

        object DeviceCallback : BluetoothAdapter.LeScanCallback {
            var cb = { device: BluetoothDevice? -> }

            override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
                cb.invoke(device)
            }
        }

        private object MyHandler : Handler()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    object NewImpl : BleScanner, Logger {
        override fun scanForName(adapter: BluetoothAdapter, name: String, duration: Long, action: (success: Boolean, device: BluetoothDevice?) -> Unit) {
            fun fail() {
                action(false, null)
                adapter.bluetoothLeScanner.stopScan(DeviceCallback)
                MyHandler.removeCallbacksAndMessages(null)
            }

            fun success(device: BluetoothDevice) {
                action(true, device)
                adapter.bluetoothLeScanner.stopScan(DeviceCallback)
                MyHandler.removeCallbacksAndMessages(null)
            }

            MyHandler.postDelayed({ fail() }, duration)

            DeviceCallback.cb = { result, scanResult ->
                if (result.not()) {
                    fail()
                } else {
                    if (scanResult != null && scanResult.device.name trimAndIgnoreCaseEquals name) {
                        success(scanResult.device)
                    }
                }
            }
            adapter.bluetoothLeScanner.startScan(DeviceCallback)
        }

        override fun isLog(): Boolean {
            return true
        }

        private val deviceList = ArrayList<BluetoothDevice>()

        override fun scanForList(adapter: BluetoothAdapter, duration: Long, action: (success: Boolean, list: List<BluetoothDevice>) -> Unit) {
            Callback.cb = action
            adapter.bluetoothLeScanner.startScan(Callback)
            val function = {
                adapter.bluetoothLeScanner.stopScan(Callback)
                MyHandler.removeCallbacksAndMessages(null)
                Callback.cb(true, deviceList.toList())
            }
            MyHandler.postDelayed(function, duration)
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

        object DeviceCallback : ScanCallback() {
            var cb = { success: Boolean, result: ScanResult? -> }

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                cb.invoke(true, result)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                cb.invoke(false, null)
            }
        }

        private object MyHandler : Handler()
    }

    interface BleScanner {
        fun scanForList(adapter: BluetoothAdapter, duration: Long = DURATION, action: (success: Boolean, list: List<BluetoothDevice>) -> Unit)

        fun scanForName(adapter: BluetoothAdapter, name: String, duration: Long = DURATION, action: (success: Boolean, device: BluetoothDevice?) -> Unit)
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

        /**
         * 没有找到对应uuid的服务,与设备的连接自动关闭
         */
        fun onNotFoundService(serviceUUID: UUID)

        /**
         * 没有找到对应的uuid的特征,与设备的连接会自动关闭
         */
        fun noNotFoundCharacteristic(characteristicUUID: UUID)

    }

    /**
     * 接口方法太多,适配器模式,只写自己需要继承的方法
     */
    open class BleStateCallbackAdapter : BleStateCallback {
        override fun onBeginStartScanDevice() {
        }

        override fun onScanEnd(device: BluetoothDevice?) {
        }

        override fun onConnect(success: Boolean) {
        }

        override fun onDisconnect() {
        }

        override fun onPrepared(device: BluetoothDevice?) {
        }

        override fun onNotFoundService(serviceUUID: UUID) {
        }

        override fun noNotFoundCharacteristic(characteristicUUID: UUID) {
        }
    }
}
