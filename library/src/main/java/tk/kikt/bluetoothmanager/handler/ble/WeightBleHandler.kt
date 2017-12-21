package tk.kikt.bluetoothmanager.handler.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.support.annotation.RequiresApi
import tk.kikt.bluetoothmanager.ext.uiThread
import tk.kikt.bluetoothmanager.handler.BluetoothType
import tk.kikt.bluetoothmanager.handler.PowerType
import tk.kikt.bluetoothmanager.handler.type.WeightPlatformDeviceType
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by cai on 2017/12/15.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
object WeightBleHandler : AbstractBleHandler() {

    private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    override fun serviceUUID(): UUID = SERVICE_UUID

    override fun characteristicUUID(): UUID = CHARACTERISTIC_UUID

    override fun handleCharacteristic(gatt: BluetoothGatt?, service: BluetoothGattService, characteristic: BluetoothGattCharacteristic): Boolean {
        gatt?.setCharacteristicNotification(characteristic, true)

        return true
    }

    override fun onNotifyChange(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        characteristic?.value?.apply {
            val value = toString(Charset.forName("gbk")).trim()
            weightCallback { callback ->
                callback.onValueNotify(value)
            }
        }
    }

    interface WeightBleCallback {

        fun onValueNotify(value: String)
    }

    private fun weightCallback(action: (callback: WeightBleCallback) -> Unit) {
        uiThread {
            weightLock.withLock {
                callback?.let {
                    action(it)
                }
                weightListenerExec {
                    action(it)
                }
            }
        }
    }

    var callback: WeightBleCallback? = null

    private val receiverWeightListenerList = arrayListOf<WeightBleCallback>()

    private var weightLock: Lock = ReentrantLock()

    fun addReceiveWeightListener(receiveWeightListener: WeightBleCallback) {
        weightLock.withLock {
            receiverWeightListenerList.add(receiveWeightListener)
        }
    }

    fun removeReceiveWeightListener(receiveWeightListener: WeightBleCallback) {
        weightLock.withLock {
            receiverWeightListenerList.remove(receiveWeightListener)
        }
    }

    private fun weightListenerExec(action: (receiveWeightListener: WeightBleCallback) -> Unit) {
        weightLock.withLock {
            receiverWeightListenerList.forEach {
                action(it)
            }
        }
    }

    override fun type() = WeightBleType

    object WeightBleType : BluetoothType {
        override val deviceType = WeightPlatformDeviceType
        override val powerType = PowerType.BLE
    }
}