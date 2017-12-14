package tk.kikt.bluetoothmanager.handler.normal

import tk.kikt.bluetoothmanager.handler.BluetoothHandler
import tk.kikt.bluetoothmanager.handler.BluetoothType
import java.nio.charset.Charset

/**
 * Created by cai on 2017/12/14.
 */
object PrinterHandler : AbstractNormalBluetoothHandler(), BluetoothHandler {
    override fun convertMsgStringToByteArray(msg: String): ByteArray {
        return msg.toByteArray(Charset.forName("gbk"))
    }

    object PrinterType : BluetoothType

    override fun onRead(byteArray: ByteArray) {
    }

    override fun type() = PrinterType
}