package com.tornadoentertainment.nfc_dsxml

import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


/**
 * Created by Kan on 2020-05-28
 * Copyright © 2018 Money Forward, Inc. All rights
 */

class CryptoUtils {
    companion object {
        @Throws(NoSuchAlgorithmException::class)
        fun SHA256(text: String): String {

            val md = MessageDigest.getInstance("SHA-256")

            md.update(text.toByteArray())
            val digest = md.digest()

            return Base64.encodeToString(digest, Base64.DEFAULT)
        }
    }
}