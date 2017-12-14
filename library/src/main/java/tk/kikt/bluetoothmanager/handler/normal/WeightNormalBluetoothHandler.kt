package tk.kikt.bluetoothmanager.handler.normal

import tk.kikt.bluetoothmanager.handler.BluetoothHandler
import tk.kikt.bluetoothmanager.handler.BluetoothType
import java.nio.charset.Charset
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by cai on 2017/12/14.
 * 普通蓝牙电子秤 8位版
 */
object WeightNormalBluetoothHandler : AbstractNormalBluetoothHandler(), BluetoothHandler {
    override fun type() = Weight

    object Weight : BluetoothType

    override fun convertMsgStringToByteArray(msg: String): ByteArray {
        return msg.toByteArray(Charset.forName("gbk"))
    }

    private val list: ArrayList<Byte> = ArrayList()

    private val lock: Lock = ReentrantLock()

    override fun onRead(byteArray: ByteArray) {
        lock.withLock {
            list.addAll(byteArray.toMutableList())
            if (list.size >= 8) {
                val weightString = list.toByteArray().toString(Charset.forName("gbk")).trim()
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