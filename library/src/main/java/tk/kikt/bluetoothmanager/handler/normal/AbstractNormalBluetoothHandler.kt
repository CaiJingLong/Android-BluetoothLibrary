package tk.kikt.bluetoothmanager.handler.normal

import android.bluetooth.BluetoothDevice
import android.os.SystemClock
import tk.kikt.bluetoothmanager.BluetoothConnectManager
import tk.kikt.bluetoothmanager.BluetoothHelper
import tk.kikt.bluetoothmanager.Logger
import tk.kikt.bluetoothmanager.handler.AbstractBluetoothHandler
import tk.kikt.bluetoothmanager.log
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by cai on 2017/12/14.
 * 抽象的普通蓝牙连接的处理类
 */
abstract class AbstractNormalBluetoothHandler : AbstractBluetoothHandler(), Logger {

    override fun isLog() = BluetoothConnectManager.isLog()

    /**
     * 连接方法
     */
    fun conn(name: String, pwd: String, init: BluetoothCallback.() -> Unit) {
        val cb = BluetoothCallback()
        cb.init()
        val outInit = cb.onConnCallback
        val outcb = BluetoothHelper.ConnectStateCallback().apply {
            outInit()
        }
        cb.onConnCallback = {
            connectSuccess = {
                outcb.connectSuccess.invoke(it)
                currentDevice = it
            }
            connectFail = {
                outcb.connectFail.invoke(it)
                currentDevice = null
            }
            connectDisconnect = {
                outcb.connectDisconnect.invoke(it)
                currentDevice = null
            }
        }

        BluetoothHelper.withOpen {
            BluetoothHelper.findDevice(name, callback = object : BluetoothHelper.OnNormalScanCallback {
                override fun onStartScan() {
                    cb.onStartScan()
                }

                override fun onNotFoundDevice() {
                    cb.onNotFountDevice()
                }

                override fun onFoundDevice(device: BluetoothDevice) {
                    inQueue.clear()
                    outQueue.clear()
                    BluetoothHelper.connectDevice(device, pwd, inQueue, outQueue, cb.onConnCallback) {
                        log("连接断开时调用")
                        readThreadPool.shutdown()

                        if (readThreadPool.isShutdown) {
                            readThreadPool = Executors.newFixedThreadPool(3)
                        }

                        inQueue.clear()
                        outQueue.clear()
                    }
                    waitForInputMsg()
                }
            })
        }

    }

    private var readThreadPool = Executors.newFixedThreadPool(3)
    private var writeThreadPool = Executors.newFixedThreadPool(3)
    private var writLock = ReentrantLock()

    private var inQueue: LinkedBlockingQueue<ByteArray> = LinkedBlockingQueue()
    private var outQueue: LinkedBlockingQueue<ByteArray> = LinkedBlockingQueue()

    var writeDuration = 20L

    fun write(msg: String) {
        writeThreadPool.execute {
            writLock.withLock {
                outQueue.offer(convertMsgStringToByteArray(msg))
                outQueue.offer(convertMsgStringToByteArray("\n"))
                SystemClock.sleep(writeDuration)
            }
        }
    }

    fun write(byteArray: ByteArray) {
        writeThreadPool.execute {
            writLock.withLock {
                outQueue.offer(byteArray)
                SystemClock.sleep(writeDuration)
            }
        }
    }

    protected abstract fun convertMsgStringToByteArray(msg: String): ByteArray

    private fun waitForInputMsg() {
        readThreadPool.execute {
            try {
                while (true) {
                    val byteArray = inQueue.take()
                    callReadByteArray(byteArray)
                    onRead(byteArray)
                }
            } catch (e: Exception) {
            }
        }
    }

    protected abstract fun onRead(byteArray: ByteArray)
}

class BluetoothCallback {

    lateinit var onConnCallback: (BluetoothHelper.ConnectStateCallback.() -> Unit)

    var onStartScan = {}

    var onNotFountDevice = {}

}
