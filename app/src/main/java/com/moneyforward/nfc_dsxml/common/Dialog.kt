package com.moneyforward.nfc_dsxml.common

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.moneyforward.nfc_dsxml.R

/**
 * Created by Kan on 2020-06-11
 * Copyright © 2018 Money Forward, Inc. All rights
 */

fun FragmentActivity.showAlert(
    title: String, message: String,
    textPositive: Int = R.string.ok,
    actionPositive: (() -> Unit)? = null,
    textNegative: Int = R.string.cancel,
    actionNegative: (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(textPositive) { _, _ -> actionPositive?.invoke() }
        .setNegativeButton(textNegative) { _, _ -> actionNegative?.invoke() }
        .show()
}


fun FragmentActivity.showAlertWithPositive(
    title: String, message: String,
    textPositive: Int = R.string.ok,
    actionPositive: (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(textPositive) { _, _ -> actionPositive?.invoke() }
        .show()
}