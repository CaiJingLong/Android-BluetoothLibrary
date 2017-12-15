package tk.kikt.bluetoothmanager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import tk.kikt.bluetoothmanager.handler.ble.AbstractBleHandler
import tk.kikt.bluetoothmanager.handler.ble.WeightBleHandler
import tk.kikt.bluetoothmanager.handler.normal.AbstractNormalBluetoothHandler
import tk.kikt.bluetoothmanager.handler.normal.PrinterHandler
import tk.kikt.bluetoothmanager.handler.normal.WeightNormalBluetoothHandler
import java.util.*

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MainActivity : AppCompatActivity(), Logger {

    override fun isLog(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BluetoothConnectManager.init(application)
        testBleConnect()
    }

    private fun testBleConnect() {
        val callback: AbstractBleHandler.BleStateCallback = object : AbstractBleHandler.BleStateCallback {
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
            WeightBleHandler.connect("BJJY-1588")
        }
    }

    private fun testConnectPrinter() {
        PrinterHandler.conn("sxw-p051", "1234") {
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

        bt_print.setOnClickListener {
            PrinterHandler.write("你好")
        }
    }

    private fun testConnectWeight() {
        WeightNormalBluetoothHandler.receiveWeightListener = object : WeightNormalBluetoothHandler.OnReceiveWeightListener {
            override fun onReceiveWeight(msg: String) {

            }
        }
    }

    object MyNormalBluetoothHandlerImpl : AbstractNormalBluetoothHandler() {
        override fun convertMsgStringToByteArray(msg: String): ByteArray {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onRead(byteArray: ByteArray) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object MyBleBluetoothHanlderImpl : AbstractBleHandler() {
        override fun serviceUUID(): UUID {
            TODO("not implemented") //return your service's UUID
        }

        override fun characteristicUUID(): UUID {
            TODO("not implemented") //return your characteristic's UUID
        }

        override fun handleCharacteristic(gatt: BluetoothGatt?, service: BluetoothGattService, characteristic: BluetoothGattCharacteristic) {
            //handle the characteristic
            //Example :
            gatt?.setCharacteristicNotification(characteristic, true)
        }

        override fun onNotifyChange(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            //on the characteristic notify
        }
    }
}
