package tk.kikt.bluetoothmanager

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import tk.kikt.bluetoothmanager.handler.OnReadCallback
import tk.kikt.bluetoothmanager.handler.ble.AbstractBleHandler
import tk.kikt.bluetoothmanager.handler.ble.WeightBleHandler
import tk.kikt.bluetoothmanager.handler.normal.PrinterHandler
import tk.kikt.bluetoothmanager.handler.normal.WeightNormalBluetoothHandler
import java.io.IOException
import java.nio.charset.Charset

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MainActivity : AppCompatActivity(), Logger {

    override fun isLog(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BluetoothConnectManager.init(application)

        BluetoothConnectManager.addOnConnectingDeviceListener(object : BluetoothConnectManager.OnConnectingDeviceListener {
            override fun onConnectDeviceChange(newDevice: BluetoothDevice?) {
                log("the new device = $newDevice")
                log("new device name = ${newDevice?.name}")
            }
        })

//        testBleConnect()
        testConnectPrinter()
    }

    private fun testBleConnect() {
        val callback: AbstractBleHandler.BleStateCallback = object : AbstractBleHandler.BleStateCallbackAdapter() {

            override fun onBeginStartScanDevice() {
                log("开始扫描设备")
            }

            override fun onScanEnd(device: BluetoothDevice?) {
                log("扫描结束 扫描到设备:$device")
            }

            override fun onConnect(success: Boolean) {
                log("连接完毕 $success")
            }

            override fun onDisconnect() {
                log("连接断开")
            }

            override fun onPrepared(device: BluetoothDevice?) {
                log("准备完毕")
            }
        }
        WeightBleHandler.addCallback(callback)
        WeightBleHandler.callback = object : WeightBleHandler.WeightBleCallback {
            override fun onValueNotify(value: String) {
                tv_notify_value.text = value
            }
        }

        bt_main.setOnClickListener {
            WeightBleHandler.connect("BJJY-1588", 5000)
        }

    }

    private fun testConnectPrinter() {
        bt_main.setOnClickListener {
            PrinterHandler.conn("sxw-p051", "0000") {
                onConnCallback = {
                    connectSuccess = {}
                    connectDisconnect = {}
                    connectFail = {}
                }

                onStartScan = {
                }

                onNotFountDevice = {

                }
            }
        }

        bt_print.setOnClickListener {
            PrinterHandler
            PrinterHandler.write(qrCode("type:6;name:BJJY-1588"))
            PrinterHandler.write("\n\n\ntype:6;name:BJJY-1588")
            PrinterHandler.write(byteArrayOf())
        }

    }

    override fun onStart() {
        super.onStart()
        PrinterHandler.addOnReadCallback(readCallback)
    }

    private val readCallback: OnReadCallback = object : OnReadCallback {
        override fun onRead(byteArray: ByteArray) {
            log("receive read $byteArray")
        }
    }

    override fun onStop() {
        super.onStop()

        PrinterHandler.removeOnReadCallback(readCallback)
    }

    @Throws(IOException::class)
    fun qrCode(qrData: String): ByteArray {
        val moduleSize = 8
        val length = qrData.gbkByteArray().size

        val list = ArrayList<Byte>()

        //打印二维码矩阵
        list.add(0x1D)// init
        list.add(107)// adjust height of barcode
        list.add(107)// adjust height of barcode
        list.add((length + 3).toByte()) // pl
        list.add(0) // ph
        list.add(49) // cn
        list.add(80) // fn
        list.add(48) //
        list.addAll(qrData.gbkByteArray().asList())

        list.add(0x1D)
        list.add(40.toByte(), 107)// list.add("(k")
        list.add(3)
        list.add(0)
        list.add(49)
        list.add(69)
        list.add(48)

        list.add(0x1D)
        list.add(40.toByte(), 107)// list.add("(k")
        list.add(3)
        list.add(0)
        list.add(49)
        list.add(67)
        list.add(moduleSize.toByte())

        list.add(0x1D)
        list.add(40.toByte(), 107)// list.add("(k")
        list.add(3) // pl
        list.add(0) // ph
        list.add(49) // cn
        list.add(81) // fn
        list.add(48) // m

        return list.toByteArray()
    }

    private fun testConnectWeight() {
        WeightNormalBluetoothHandler.receiveWeightListener = object : WeightNormalBluetoothHandler.OnReceiveWeightListener {
            override fun onReceiveWeight(msg: String) {

            }
        }
    }
}

private fun <E> MutableList<E>.add(vararg elements: E) {
    this.addAll(elements)
}

private fun String.gbkByteArray(): ByteArray {
    return toByteArray(Charset.forName("gbk"))
}
