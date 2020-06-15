package com.moneyforward.nfc_dsxml

import android.nfc.tech.IsoDep
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.security.cert.X509Certificate
import kotlin.math.ceil

/**
 * Created by Kan on 2020-05-28
 * Copyright © 2018 Money Forward, Inc. All rights
 */


class NfcUtils {

    companion object {

        private val BLOCK_LENGTH = 256

        val commandSelectFile = byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x0C,
            0x0A,
            0xD3.toByte(),
            0x92.toByte(),
            0xF0.toByte(),
            0x00,
            0x26,
            0x01,
            0x00,
            0x00,
            0x00,
            0x01
        )


        val commandSelectCertificate = byteArrayOf(
            0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x00, 0x0A
        )

        val commandToRead = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x04)

        val commandToReadBlock = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)

        var commandSelectFilePin = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x00, 0x18)

        var commandPINVerify =
            byteArrayOf(0x00, 0x20, 0x00, 0x80.toByte())

        var commandKeyForSignature = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x00, 0x1A)

        var commandSignatureDataHeader = byteArrayOf(0x80.toByte(), 0x2A, 0x00, 0x80.toByte())

        var commandSelectComputerDigitalFile = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x00, 0x17)

        var sha1asn = byteArrayOf(0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a)

        fun commandSignatureData(data: ByteArray): ByteArray {
            var result: ArrayList<Byte> = ArrayList()
            result.addAll(commandSignatureDataHeader.asList())
            result.add(data.size.toByte())
            result.addAll(data.asList())
            return result.toByteArray()
        }

        fun commandSignaturePin(data: ByteArray): ByteArray {
            var result: ArrayList<Byte> = ArrayList()
            result.addAll(commandPINVerify.asList())
            result.add(data.size.toByte())
            result.addAll(data.asList())
            return result.toByteArray()
        }

        fun asn1DigestInfo(data: ByteArray): ByteArray {
            var result: ArrayList<Byte> = ArrayList()
            result.addAll(commandSignatureDataHeader.asList())
            result.add(data.size.toByte())
            result.addAll(data.asList())
            return result.toByteArray()
        }

        private fun certificateFromString(data: ByteArray): Certificate? {

            val stream = ByteArrayInputStream(data)

            return CertificateFactory.getInstance("X.509").generateCertificate(stream)
        }

        fun ReadData(
            isodep: IsoDep
        ): ByteArray {
            var readLength = 0
            var ret: ByteArray = byteArrayOf()
            var outbyte: ArrayList<Byte> = ArrayList()
            try {

                val response = isodep.transceive(commandToRead)
                if(response.size <=2)
                    return response
                readLength = Utils.bytesToUnsignedShort(
                    response[2],
                    response[3],
                    true
                )
                val blockNum = ceil(readLength / BLOCK_LENGTH.toDouble()).toInt()
                var cmd = commandToReadBlock
                for (index in 0..blockNum) {
                    cmd[2] = index.toByte()
                    ret = isodep.transceive(cmd)
                    outbyte.addAll(ret.asList())
                    if (ret.size <= 2) {
                        break
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return outbyte.toByteArray()
        }

    }


}