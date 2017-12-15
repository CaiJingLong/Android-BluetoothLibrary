# Android-BluetoothLibrary

## install
in your's project root path
```gradle
buildscript {
    ext.kotlin_version = '1.2.0'
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.0'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"

        classpath 'com.github.dcendents:android-maven-gradle-plugin:1.4.1' // copy to your file
    }
}


allprojects {
    repositories {
        google()
        jcenter()
        ...
        maven { url 'https://jitpack.io' } //copy to your project
    }
}
```

in your application module's build.gradle
```gradle
dependencies {
    implementation 'com.github.CaiJingLong:Android-BluetoothLibrary:0.9.1'
}
```

## use
the ble bluetooth
```kotlin
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
```

for the bluetooth version 2.0 's Printer
```kotlin
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
```

## extension

### Bluetooth 2.0
```kotlin

 object MyNormalBluetoothHandlerImpl:AbstractNormalBluetoothHandler(){
        override fun convertMsgStringToByteArray(msg: String): ByteArray {
            TODO("not implemented") //the result is convert your string to the ByteArray
        }

        override fun onRead(byteArray: ByteArray) {
            TODO("not implemented") //on receive the ByteArray from bluetooth devices
        }
    }
```

### BLE
```kotlin
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
```