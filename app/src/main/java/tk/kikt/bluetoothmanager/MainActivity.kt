package tk.kikt.bluetoothmanager

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import tk.kikt.bluetoothmanager.handler.BluetoothType
import tk.kikt.bluetoothmanager.handler.DeviceType
import tk.kikt.bluetoothmanager.handler.OnReadCallback
import tk.kikt.bluetoothmanager.handler.PowerType
import tk.kikt.bluetoothmanager.handler.ble.AbstractBleHandler
import tk.kikt.bluetoothmanager.handler.ble.WeightBleHandler
import tk.kikt.bluetoothmanager.handler.normal.AbstractNormalBluetoothHandler
import tk.kikt.bluetoothmanager.handler.normal.PrinterHandler
import tk.kikt.bluetoothmanager.handler.normal.WeightNormalBluetoothHandler
import java.io.IOException
import java.nio.charset.Charset

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MainActivity : AppCompatActivity(), Logger, OnReadCallback {

    override fun isLog(): Boolean {
        return true
    }

    object TestHandler : AbstractNormalBluetoothHandler() {
        val sb = StringBuilder()

        override fun type() = object : BluetoothType {
            override val deviceType: DeviceType
                get() = object : DeviceType {}

            override val powerType: PowerType
                get() = PowerType.NORMAL
        }

        override fun convertMsgStringToByteArray(msg: String): ByteArray {
            return msg.toByteArray()
        }

        override fun onRead(byteArray: ByteArray) {
            byteArray.forEach {
                sb.append(it, ",")
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BluetoothConnectManager.init(application)

//        BluetoothConnectManager.addOnConnectingDeviceListener(object : BluetoothConnectManager.OnConnectingDeviceListener {
//            override fun onConnectDeviceChange(newDevice: BluetoothDevice?) {
//                log("the new device = $newDevice")
//                log("new device name = ${newDevice?.name}")
//            }
//        })

//        testBleConnect()
//        testConnectPrinter()

        PrinterHandler.addOnReadCallback(this)
        bt_print.setOnClickListener {
            PrinterHandler.write(byteArrayOf(0x1D, 0x67, 0x69))
            Log.i(TAG, "发送指令完毕")
        }

        PrinterHandler.conn("sxw-p051", "0000", {
            onConnCallback = {
                connectSuccess = {
                    Log.i(TAG, "连接 ${it.name}成功")
                }
            }
        })
    }

    private val TAG = "MainActivity";

    override fun onRead(byteArray: ByteArray) {
        Log.i(TAG, "${byteArray.toMutableList()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        PrinterHandler.removeOnReadCallback(this)
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
            val byteArray = qrCode2("type:6;name:BJJY-1588")
            val msg = "\n\n\ntype:6;name:BJJY-1588".gbkByteArray()

            val target = arrayListOf<Byte>().apply {
                addAll(byteArray.toList())
                addAll(msg.toList())
            }.toByteArray()

            println("byteArray = ${byteArray.asList()}")
            println("target = ${target.asList()}")
//            PrinterHandler.write(target)
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

    /**
     * @param qrData 二维码内容
     * @param moduleSize 大小,默认是6
     */
    @Throws(IOException::class)
    fun qrCode2(qrData: String, moduleSize: Int = 6): ByteArray {
        val length = qrData.gbkByteArray().size

        val list = ArrayList<Byte>()

        //二维码像素点大小
        list.addInt(0x1B, 0x23, 0x23, 0x51, 0x50, 0x49, 0x58, moduleSize)

        //单元大小 GS	   (     k 	  pL    pH     1     C   n
        list.addInt(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, moduleSize)

        //设置错误纠错等级
        //GS	  (    k 	  pL    pH     1     E   n
        list.addInt(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x49)

        //传输数据到编码缓存
        // GS	  (    k 	  pL    pH     1     P   m   d0....dk
        // 1D 	 28   6B      03    00     31    50  m   d0...dk
        //pL pH, 为后续数据长度
        // (pL + pH×256) = len + 3， len 为编码数据长度
        list.addInt(0x1D, 0x28, 0x6B, length + 3, 0x00, 0x31, 0x50, 0x30)
        list.addAll(qrData.gbkByteArray().asList())//数据

        //打印缓存
        // GS	   (    k 	  pL    pH     1     Q   m
        // 1D 	  28   6B     03    00     31    51   m
        list.addInt(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)

        return list.toByteArray()
    }

    private fun ArrayList<Byte>.addInt(vararg i: Int) {
        i.forEach { add(it.toByte()) }
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
