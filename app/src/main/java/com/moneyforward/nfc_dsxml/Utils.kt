package com.moneyforward.nfc_dsxml

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Files.exists


/**
 * Created by Kan on 2020-05-28
 * Copyright © 2018 Money Forward, Inc. All rights
 */

class Utils {

    companion object {
        private val HEX_CHARS = "0123456789ABCDEF"

        fun hexStringToByteArray(data: String): ByteArray {

            val result = ByteArray(data.length / 2)

            for (i in 0 until data.length step 2) {
                val firstIndex = HEX_CHARS.indexOf(data[i]);
                val secondIndex = HEX_CHARS.indexOf(data[i + 1]);

                val octet = firstIndex.shl(4).or(secondIndex)
                result.set(i.shr(1), octet.toByte())
            }

            return result
        }

        private val HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray()
        fun toHex(byteArray: ByteArray): String {
            val result = StringBuffer()

            byteArray.forEach {
                val octet = it.toInt()
                val firstIndex = (octet and 0xF0).ushr(4)
                val secondIndex = octet and 0x0F
                result.append(HEX_CHARS_ARRAY[firstIndex])
                result.append(HEX_CHARS_ARRAY[secondIndex])
            }

            return result.toString()
        }

        fun bytesToUnsignedShort(byte1: Byte, byte2: Byte, bigEndian: Boolean): Int {
            if (bigEndian)
                return (((byte1.toInt() and 255) shl 8) or (byte2.toInt() and 255))


            return (((byte2.toInt() and 255) shl 8) or (byte1.toInt() and 255))

        }

        fun writeFileOnInternalStorage(mcoContext: File, sFileName: String, sBody: String) {
            val file = File(mcoContext, sFileName)
            if (!file.exists()) {
                file.mkdir()
            }

            try {
                val fos = FileOutputStream(file)
                fos.write(sBody.toByteArray())
                fos.close()

            } catch (e: Exception) {
                e.printStackTrace()

            }

        }
    }
}