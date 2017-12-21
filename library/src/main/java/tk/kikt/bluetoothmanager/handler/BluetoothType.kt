package tk.kikt.bluetoothmanager.handler

/**
 * Created by cai on 2017/12/14.
 * 标识蓝牙类型,由子类自己实现
 */
interface BluetoothType {

    val deviceType: DeviceType

    val powerType: PowerType

}

/**
 * Created by cai on 2017/12/21.
 * 设备类型
 */
interface DeviceType


/**
 * Created by cai on 2017/12/21.
 * 消耗能量类型
 */
//interface PowerType


sealed class PowerType {
    object NORMAL : PowerType()
    object BLE : PowerType()
}