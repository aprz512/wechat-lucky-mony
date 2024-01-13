package com.aprz.wechatredenvelope.utils

import android.widget.Toast
import androidx.annotation.StringRes
import com.aprz.wechatredenvelope.App

fun shortToast(msg: String) {
    Toast.makeText(App.instance, msg, Toast.LENGTH_SHORT).show()
}

fun shortToast(@StringRes id: Int) {
    val msg = App.instance.getString(id)
    Toast.makeText(App.instance, msg, Toast.LENGTH_SHORT).show()
}


