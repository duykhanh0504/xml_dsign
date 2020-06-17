package com.moneyforward.nfc_dsxml

/**
 * Created by Kan on 2020-06-16
 * Copyright © 2018 Money Forward, Inc. All rights
 */

enum class SHAAlgorithm(val index: Int) {
    SHA1(0),
    SHA256(1),
    ERROR(-1);
}