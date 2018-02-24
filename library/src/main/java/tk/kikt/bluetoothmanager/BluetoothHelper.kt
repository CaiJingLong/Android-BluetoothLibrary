package tk.kikt.bluetoothmanager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.widget.Toast
import tk.kikt.bluetoothmanager.ext.checkShutdown
import tk.kikt.bluetoothmanager.ext.trimAndIgnoreCaseEquals
import tk.kikt.bluetoothmanager.ext.uiThread
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue


/**
 * Created by cai on 2017/12/14.
 * 蓝牙连接的帮助类
 */
object BluetoothHelper : Logger {
    var showLog = true

    override fun isLog() = BluetoothConnectManager.isLog()

    //等待蓝牙开关开启,因为某些资源未释放或别的原因造成的蓝牙连接不成功,使用先关闭蓝牙开关,由系统释放的方案来解决,然后监听开启蓝牙的广播,在其中去写业务逻辑
    @JvmStatic
    inline fun withOpen(crossinline action: () -> Unit) {
        checkBluetoothAdapter { context, adapter ->
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent?) {
                    if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                        return
                    }
                    val a = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    log(a)
                    when (a) {
                        BluetoothAdapter.STATE_OFF -> adapter.enable()
                        BluetoothAdapter.STATE_ON -> {
                            context.unregisterReceiver(this)
                            action()
                        }
                    }
                }
            }, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

            if (adapter.isEnabled) {
                adapter.disable()
            } else {
                adapter.enable()
            }
        }
    }

    //蓝牙设备的UUID,默认值,可以根据需要在连接蓝牙设备时传入
    val _UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    //取消配对的广播,可能不生效,隐藏API
    const val PAIR_CANCEL = "android.bluetooth.device.action.PAIRING_CANCEL"

    //申请蓝牙配对的广播,API19以下可能不生效
    const val PAIR_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST"

    //获取intent中的蓝牙设备
    private fun getDevice(intent: Intent?): BluetoothDevice? {
        return intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }

    /**
     * @param name 蓝牙设备的名称
     * @param delayOff 关闭延迟 单位毫秒
     * @param callback 蓝牙连接相关的一些回调
     */
    fun findDevice(name: String, delayOff: Long = 20000, callback: OnNormalScanCallback) {
        checkBluetoothAdapter { context, adapter ->

            val handler = Handler()

            val filter = IntentFilter().apply {
                //扫描相关
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)

                //扫描结果后
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            }

            var findDevice: BluetoothDevice? = null

            context.registerReceiver(object : BroadcastReceiver() {

                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {//扫描开始
                            callback.onStartScan()
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {//扫描结束
                            if (findDevice != null) {
                                callback.onFoundDevice(findDevice!!)
                            } else {
                                callback.onNotFoundDevice()
                            }
                            context?.unregisterReceiver(this)
                        }
                        BluetoothDevice.ACTION_FOUND, BluetoothDevice.ACTION_NAME_CHANGED -> {//找到设备,找到设备后由mac地址改为设备名称
                            val device = getDevice(intent)
                            if (device?.name?.trim() trimAndIgnoreCaseEquals name.trim()) {
                                findDevice = device
                                adapter.cancelDiscovery()
                            }
                        }
                    }
                }
            }, filter)

            handler.postDelayed({
                adapter.cancelDiscovery()
            }, delayOff)

            adapter.startDiscovery()
        }
    }

    var useInsecureConnect = true

    /**
     * 连接蓝牙设备的方法
     * @param inQueue 读取的队列
     * @param outQueue 输出的队列
     */
    fun connectDevice(device: BluetoothDevice, pwd: String = "1234", inQueue: LinkedBlockingQueue<ByteArray>, outQueue: LinkedBlockingQueue<ByteArray>, init: ConnectStateCallback.() -> Unit, uuid: UUID = _UUID, onShutDown: () -> Unit) {
        val threadPool = Executors.newFixedThreadPool(3)

        checkBluetoothAdapter { context, adapter ->
            withBind(device, pwd) {
                val cb = ConnectStateCallback()
                cb.init()
                log("准备连接")
                threadPool.execute(Runnable {
                    var socket = if (!useInsecureConnect) device.createRfcommSocketToServiceRecord(uuid) else device.createInsecureRfcommSocketToServiceRecord(uuid)

                    try {
                        socket.connect()
                        log("连接成功")
                        cb.connectSuccess(device)
                    } catch (e: Exception) {
                        socket = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(device, 1) as BluetoothSocket
                        try {
                            socket.connect()
                        } catch (e: Exception) {
                            log("连接出错", e)
                            cb.connectFail(device)
                            return@Runnable
                        }
                    }

                    uiThread {
                        threadPool.execute {
                            //读取相关
                            val input: InputStream?
                            try {
                                input = socket.inputStream
                            } catch (e: Exception) {
                                cb.connectFail(device)
                                threadPool.checkShutdown {
                                    onShutDown()
                                }
                                return@execute
                            }
                            try {
                                val buf = ByteArray(1024)
                                var len = 0
                                while (true) {
                                    len = input.read(buf)
                                    if (len > 0) {
                                        inQueue.offer(Arrays.copyOf(buf, len))
                                    }
                                }
                            } catch (e: Exception) {
                                cb.connectDisconnect(device)
                                threadPool.checkShutdown {
                                    onShutDown()
                                }
                            }
                        }

                        threadPool.execute {
                            val output: OutputStream?
                            try {
                                output = socket.outputStream
                            } catch (e: Exception) {
                                cb.connectFail(device)
                                threadPool.checkShutdown {
                                    onShutDown()
                                }
                                return@execute
                            }
                            try {
                                while (true) {
                                    val bytes = outQueue.take()
                                    output.write(bytes)
                                    output.flush()
                                }
                            } catch (e: Exception) {
                                cb.connectDisconnect(device)
                                threadPool.checkShutdown {
                                    onShutDown()
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    /**
     * 连接蓝牙设备的方法
     * @param inQueue 读取的队列
     * @param outQueue 输出的队列
     */
    fun connectDevice(device: BluetoothDevice, pwd: String = "1234", inQueue: LinkedBlockingQueue<ByteArray>, outQueue: LinkedBlockingQueue<ByteArray>, init: ConnectStateCallback.() -> Unit, uuid: String, onShutDown: () -> Unit) {
        connectDevice(device, pwd, inQueue, outQueue, init, UUID.fromString(uuid), onShutDown)
    }

    //绑定蓝牙相关的方法
    fun withBind(device: BluetoothDevice, pwd: String, action: () -> Unit) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            log("${device.name} 已绑定")
            action()
            return
        }
        log("${device.name} 未绑定")
        checkBluetoothAdapter { context, adapter ->
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
                }
            }

            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BluetoothDevice.ACTION_PAIRING_REQUEST -> {//请求配对
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //API19 以下不支持这个广播
                                log("请求配对")
                                toast("正在请求配对")
                                if (device.setPin(pwd.toByteArray())) {
                                    context?.unregisterReceiver(this)
                                    action()
                                }
                            }
                        }
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {//绑定状态改变
                            if (getDevice(intent)?.bondState == BluetoothDevice.BOND_BONDED) {
                                toast("已绑定")
                                log("已绑定")
                                context?.unregisterReceiver(this)
                            } else {
                                toast("未绑定")
                                log("未绑定")
                            }
                        }
                        PAIR_CANCEL -> {//配对取消
                            toast("配对取消")
                            log("配对取消")
                            context?.unregisterReceiver(this)
                        }
                    }
                }
            }, filter)

            realBind(device)
        }
    }

    private fun realBind(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            device.createBond()
        } else {
            val method = device.javaClass.getMethod("createBond")
            method.invoke(device)
        }
    }


    inline fun checkBluetoothAdapter(crossinline action: (context: Context, adapter: BluetoothAdapter) -> Unit) {
        val context = BluetoothConnectManager.application
        val adapter = BluetoothConnectManager.adapter
        if (adapter == null) {
            toast("没有找到蓝牙设备")
            return
        }
        action(context, adapter)
    }

    interface OnNormalScanCallback {
        fun onStartScan()

        fun onNotFoundDevice()

        fun onFoundDevice(device: BluetoothDevice)
    }

    class ConnectStateCallback {

        var connectSuccess = { device: BluetoothDevice -> }

        var connectFail = { device: BluetoothDevice -> }

        var connectDisconnect = { device: BluetoothDevice -> }

    }

    private val handler = Handler()

    fun toast(msg: String) {
        handler.post {
            val context = BluetoothConnectManager.application
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

}