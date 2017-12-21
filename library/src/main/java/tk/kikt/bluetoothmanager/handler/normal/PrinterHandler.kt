package tk.kikt.bluetoothmanager.handler.normal

import tk.kikt.bluetoothmanager.handler.BluetoothHandler
import tk.kikt.bluetoothmanager.handler.BluetoothType
import tk.kikt.bluetoothmanager.handler.PowerType
import tk.kikt.bluetoothmanager.handler.type.PrinterDeviceType
import java.nio.charset.Charset

/**
 * Created by cai on 2017/12/14.
 */
object PrinterHandler : AbstractNormalBluetoothHandler(), BluetoothHandler {
    override fun convertMsgStringToByteArray(msg: String): ByteArray {
        return msg.toByteArray(Charset.forName("gbk"))
    }

    override fun onRead(byteArray: ByteArray) {
    }

    override fun type() = PrinterType

    object PrinterType : BluetoothType {
        override val deviceType = PrinterDeviceType
        override val powerType = PowerType.NORMAL
    }
}
