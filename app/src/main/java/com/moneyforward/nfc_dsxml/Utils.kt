package com.moneyforward.nfc_dsxml


import android.content.Context
import android.util.Log
import java.io.*
import java.math.BigInteger
import kotlin.experimental.and
import android.R.attr.path
import android.R.attr.seekBarStyle
import java.nio.charset.Charset


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
                file.createNewFile()
            }
            val filePath = file.absolutePath
            val UTF8 = Charsets.UTF_8

            val bw = OutputStreamWriter(FileOutputStream(filePath), UTF8)
            try {
                bw.write(String(sBody.toByteArray(), UTF8))

                bw.close()

            } catch (e: Exception) {
                e.printStackTrace()

            }

        }


        fun getHex(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (i in bytes.indices.reversed()) {
                val b = bytes[i] and 0xff.toByte()
                if (b < 0x10)
                    sb.append('0')
                sb.append(Integer.toHexString(b.toInt()))
                if (i > 0) {
                    sb.append(" ")
                }
            }
            return sb.toString()
        }

        fun getDec(bytes: ByteArray): Long {
            var result: Long = 0
            var factor: Long = 1
            for (i in bytes.indices) {
                val value = bytes[i] and 0xffL.toByte()
                result += value * factor
                factor *= 256L
            }
            return result
        }

        fun getReversed(bytes: ByteArray): Long {
            var result: Long = 0
            var factor: Long = 1
            for (i in bytes.indices.reversed()) {
                val value = bytes[i] and 0xffL.toByte()
                result += value * factor
                factor *= 256L
            }
            return result
        }


        fun ByteArrayToHexString(inarray: ByteArray): String {

            Log.d("ByteArrayToHexString", inarray.toString())

            var i: Int
            var j: Int
            var `in`: Int
            val hex =
                arrayOf(
                    "0",
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "8",
                    "9",
                    "A",
                    "B",
                    "C",
                    "D",
                    "E",
                    "F"
                )
            var out = ""

            j = 0
            while (j < inarray.size) {
                `in` = inarray[j].toInt() and 0xff
                i = `in` shr 4 and 0x0f
                out += hex[i]
                i = `in` and 0x0f
                out += hex[i]
                ++j
            }
            //CE7AEED4
            //EE7BEED4
            Log.d(
                "ByteArrayToHexString",
                String.format("%0" + inarray.size * 2 + "X", BigInteger(1, inarray))
            )


            return out
        }
    }
}