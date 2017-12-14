package tk.kikt.bluetoothmanager

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import tk.kikt.bluetoothmanager.handler.ble.AbstractBleHandler
import tk.kikt.bluetoothmanager.handler.normal.PrinterHandler

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BluetoothConnectManager.init(application)
        bt_main.setOnClickListener {
            //            testConnectPrinter()
            object : AbstractBleHandler() {}.connect("abc")
        }

        bt_print.setOnClickListener {
            PrinterHandler.write("你好")
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
    }
}
