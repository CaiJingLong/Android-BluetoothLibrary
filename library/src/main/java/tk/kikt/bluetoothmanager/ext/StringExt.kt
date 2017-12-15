package tk.kikt.bluetoothmanager.ext

/**
 * Created by cai on 2017/12/15.
 */
infix fun String?.ignoreCaseEquals(to: String?) = this?.toLowerCase()?.trim() == to?.toLowerCase()?.trim()

infix fun String?.trimAndIgnoreCaseEquals(to: String?) = this?.trim() ignoreCaseEquals to?.trim()