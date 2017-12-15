package tk.kikt.bluetoothmanager.handler.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.support.annotation.RequiresApi
import tk.kikt.bluetoothmanager.ext.uiThread
import java.nio.charset.Charset
import java.util.*

/**
 * Created by cai on 2017/12/15.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
object WeightBleHandler : AbstractBleHandler() {

    private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    override fun serviceUUID(): UUID = SERVICE_UUID

    override fun characteristicUUID(): UUID = CHARACTERISTIC_UUID

    override fun handleCharacteristic(gatt: BluetoothGatt?, service: BluetoothGattService, characteristic: BluetoothGattCharacteristic) {
        gatt?.setCharacteristicNotification(characteristic, true)
        callbackExec { it.onPrepared(gatt?.device) }
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

    inline fun weightCallback(crossinline action: (callback: WeightBleCallback) -> Unit) {
        uiThread {
            callback?.let {
                action(it)
            }
        }
    }

    var callback: WeightBleCallback? = null
}