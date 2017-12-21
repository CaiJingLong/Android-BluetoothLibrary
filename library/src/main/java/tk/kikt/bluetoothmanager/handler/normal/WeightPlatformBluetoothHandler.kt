package tk.kikt.bluetoothmanager.handler.normal

import tk.kikt.bluetoothmanager.handler.BluetoothHandler
import tk.kikt.bluetoothmanager.handler.BluetoothType
import tk.kikt.bluetoothmanager.handler.PowerType
import tk.kikt.bluetoothmanager.handler.type.WeightPlatformDeviceType
import java.nio.charset.Charset
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by cai on 2017/12/14.
 * 普通蓝牙电子秤 16位版本(克重称)
 */
object WeightPlatformBluetoothHandler : AbstractNormalBluetoothHandler(), BluetoothHandler {
    override fun type() = Weight

    object Weight : BluetoothType {
        override val deviceType = WeightPlatformDeviceType
        override val powerType = PowerType.NORMAL
    }

    override fun convertMsgStringToByteArray(msg: String): ByteArray {
        return msg.toByteArray(Charset.forName("gbk"))
    }

    private val list: ArrayList<Byte> = ArrayList()

    private val lock: Lock = ReentrantLock()

    override fun onRead(byteArray: ByteArray) {
        lock.withLock {
            list.addAll(byteArray.toMutableList())
            if (list.size >= 16) {
                val weightString = list.toByteArray().toString(Charset.forName("gbk")).replace("enter.", "", true).trim()
                //业务逻辑
                receiveWeightListener?.onReceiveWeight(weightString)
                list.clear()
            }
        }
    }

    interface OnReceiveWeightListener {
        fun onReceiveWeight(msg: String)
    }

    var receiveWeightListener: OnReceiveWeightListener? = null
}