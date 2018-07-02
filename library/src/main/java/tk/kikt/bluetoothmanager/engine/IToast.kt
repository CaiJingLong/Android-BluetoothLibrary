package tk.kikt.bluetoothmanager.engine

import android.widget.Toast

/**
 * Created by cai on 2018/7/2.
 */
interface IToast {

    fun toast(msg: String, length: Int = Toast.LENGTH_SHORT)

}