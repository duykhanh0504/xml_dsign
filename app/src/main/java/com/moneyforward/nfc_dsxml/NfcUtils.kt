package com.moneyforward.nfc_dsxml

import android.nfc.tech.IsoDep
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
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

        var commandSelectComputerDigitalFile =
            byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x00, 0x17)

        // <OBJECT IDENTIFIER>
        // 01    : TAG             = OID(OBJECT IDENTIFIER) = 0x06
        // 02    : Length of Value = 5byte = 0x05
        // 03-07 : Value           = 1,3,14,3,2,26 -> SHA1 = 0x2b 0e 03 02 1a
        var objectIndentifySha1 = byteArrayOf(0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a)

        var tagSequenceData = byteArrayOf(0x05, 0x00)

        fun commandSignatureData(data: ByteArray): ByteArray {
            var result: ArrayList<Byte> = ArrayList()
            result.addAll(commandSignatureDataHeader.asList())
            result.add(data.size.toByte())
            result.addAll(data.asList())
            result.add(0x00.toByte())
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
            val result: ArrayList<Byte> = ArrayList()
            val sequence: ArrayList<Byte> = ArrayList()
            val digist: ArrayList<Byte> = ArrayList()

            // 01    : TAG             = SEQUENCE= 0x30
            // 02    : Length of Value = length(OBJECT IDENTIFIER+NULL)
            // 03-   : Value           = OBJECT IDENTIFIER+NULL

            sequence.add(0x30)
            sequence.add(((objectIndentifySha1.size + tagSequenceData.size).toByte()))
            sequence.addAll(objectIndentifySha1.asList())
            sequence.addAll(tagSequenceData.asList())

            // <OCTET STRING>
            // 01    : TAG             = OCTET STRING   = 0x04
            // 02    : Length of Value = length(digest)
            // 03-   : Value           = digest

            digist.add(0x04)
            digist.add(data.size.toByte())
            digist.addAll(data.asList())

            // <DigestInfo>
            // 01    : TAG             = SEQUENCE= 0x30
            // 02    : Length of Value = length(SEQUENCE+digest)
            // 03-   : Value           = SEQUENCE+digest

            result.add(0x30)
            result.add((sequence.size + digist.size).toByte())
            result.addAll(sequence)
            result.addAll(digist)

            return result.toByteArray()
        }

        fun certificateFromString(data: ByteArray): Certificate? {

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
                if (response.size <= 2)
                    return response
                readLength = Utils.bytesToUnsignedShort(
                    response[2],
                    response[3],
                    true
                ) + 4
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