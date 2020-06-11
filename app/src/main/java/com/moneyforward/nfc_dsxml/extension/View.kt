package com.moneyforward.nfc_dsxml.extension

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Created by Kan on 2020-06-11
 * Copyright © 2018 Money Forward, Inc. All rights
 */

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}