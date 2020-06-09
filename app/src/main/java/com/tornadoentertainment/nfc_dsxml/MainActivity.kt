package com.tornadoentertainment.nfc_dsxml

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import java.lang.Exception
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and
import android.nfc.tech.NfcF
import android.os.Environment
import android.util.Base64
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var mTag: Tag? = null

    // list of NFC technologies detected:
    private val techList = arrayOf(
        arrayOf(
            NfcA::class.java.name,
            NfcB::class.java.name,
            NfcF::class.java.name,
            NfcV::class.java.name,
            IsoDep::class.java.name,
            MifareClassic::class.java.name,
            MifareUltralight::class.java.name,
            Ndef::class.java.name
        )
    )

    val list: List<String> = listOf(
        Manifest.permission.NFC,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(this, list.toTypedArray(), 23)
        setContentView(R.layout.activity_main)
        //  textView.text = "onCreate:"
        var xml = ""
        XmlUtils.getXmltoByteArray(this, "test.xml")?.let {
            xml = XmlUtils.getXMLSignature(
                it,
                XmlUtils.createXmlSignature("", "", "")
            )
        }

        webView.loadData(xml, "text/html", null)

        val s = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        Utils.writeFileOnInternalStorage(s, "example_sign.xml", xml)


        var digistInfo: ByteArray = byteArrayOf()
        var digestValue = ""

        val xmlString = XmlUtils.getXml(this, "test.xml")
        xmlString?.toByteArray()?.let {
            val test = XmlUtils.canonicalizerXml(it)
            digistInfo = XmlUtils.createDigistInfo(test)
            val s = Base64.encodeToString(digistInfo, Base64.NO_WRAP)
            Utils.toHex(digistInfo)
            digestValue = XmlUtils.createDigistInfoBase64(test)
        }

        btnSendCommand.setOnClickListener {
            mTag?.also {
                try {

                    // val nfcf = NfcF.get(it)
                    //nfcf.connect()

                    val isoDep = IsoDep.get(it)
                    isoDep.connect()

                    val response1 = isoDep.transceive(
                        NfcUtils.commandSelectFile
                    )
                    if (response1 != byteArrayOf(0x90.toByte(), 0x00)) {

                    }
                    textView.text = "Card Response 1: " + Utils.toHex(response1)

                    val response4 = isoDep.transceive(
                        NfcUtils.commandSelectCertificate
                    )
                    textView.text = "Card Response 1: " + Utils.toHex(response4)

                    val response5 = NfcUtils.ReadData(isoDep)

                    textView.text = "Card Response 1: " + Utils.toHex(response5)


                    val response2 = isoDep.transceive(
                        NfcUtils.commandSelectFilePin
                    )
                    textView.text = "Card Response 2: " + Utils.toHex(response2)

                    val commandVerifyPin =
                        NfcUtils.commandSignaturePin(inputPin.text.toString().toByteArray())

                    val response3 = isoDep.transceive(
                        commandVerifyPin
                    )
                    textView.text = "Card Response 3: " + Utils.toHex(response3)

                    val command = NfcUtils.commandSignatureData(digistInfo)

                    val response = isoDep.transceive(command)

                    textView.text = "Card Response: " + Utils.toHex(response)

                    isoDep.close()

                    //  isoDep.close()
                } catch (e: Exception) {
                    Log.d("error", e.message)
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("onResume", "1")

        //mTextView.setText("onResume:");
        // creating pending intent:
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0
        )
        // creating intent receiver for NFC events:
        val filter = IntentFilter()
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED)
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
        // enabling foreground dispatch for getting intent from NFC event:
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, arrayOf(filter), this.techList)
    }

    override fun onPause() {
        super.onPause()

        Log.d("onPause", "1")

        // disabling foreground dispatch:
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        Log.d("onNewIntent", "1")

        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            Log.d("onNewIntent", "2")
            textView!!.setText(
                "NFC Tag\n" + ByteArrayToHexString(
                    intent.getByteArrayExtra(
                        NfcAdapter.EXTRA_ID
                    )!!
                )
            )

            //if(getIntent().hasExtra(NfcAdapter.EXTRA_TAG)){

            val tagN = intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG)
            if (tagN != null) {
                Log.d("", "Parcelable OK")
                val msgs: Array<NdefMessage>
                val empty = ByteArray(0)
                val id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
                val payload = dumpTagData(tagN).toByteArray()
                val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload)
                val msg = NdefMessage(arrayOf(record))
                msgs = arrayOf(msg)

                //Log.d(TAG, msgs[0].toString());


            } else {
                Log.d("", "Parcelable NULL")
            }


            val messages1 = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (messages1 != null) {
                Log.d("", "Found " + messages1.size + " NDEF messages")
            } else {
                Log.d("", "Not EXTRA_NDEF_MESSAGES")
            }

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            mTag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val ndef = Ndef.get(tag)
            if (ndef != null) {

                Log.d("onNewIntent:", "NfcAdapter.EXTRA_TAG")

                val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                if (messages != null) {
                    Log.d("", "Found " + messages.size + " NDEF messages")
                }
            } else {
                Log.d("", "Write to an unformatted tag not implemented")
            }


            //mTextView.setText( "NFC Tag\n" + ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_TAG)));
        }
    }

    private fun dumpTagData(p: Parcelable): String {
        val sb = StringBuilder()
        val tag = p as Tag
        val id = tag.id
        sb.append("Tag ID (hex): ").append(getHex(id)).append("\n")
        sb.append("Tag ID (dec): ").append(getDec(id)).append("\n")
        sb.append("ID (reversed): ").append(getReversed(id)).append("\n")


        val prefix = "android.nfc.tech."
        sb.append("Technologies: ")
        for (tech in tag.techList) {
            sb.append(tech.substring(prefix.length))
            sb.append(", ")
        }
        sb.delete(sb.length - 2, sb.length)
        for (tech in tag.techList) {
            if (tech == MifareClassic::class.java.name) {
                sb.append('\n')
                val mifareTag = MifareClassic.get(tag)
                var type = "Unknown"
                when (mifareTag.type) {
                    MifareClassic.TYPE_CLASSIC -> type = "Classic"
                    MifareClassic.TYPE_PLUS -> type = "Plus"
                    MifareClassic.TYPE_PRO -> type = "Pro"
                }
                sb.append("Mifare Classic type: ")
                sb.append(type)
                sb.append('\n')

                sb.append("Mifare size: ")
                sb.append(mifareTag.size.toString() + " bytes")
                sb.append('\n')

                sb.append("Mifare sectors: ")
                sb.append(mifareTag.sectorCount)
                sb.append('\n')

                sb.append("Mifare blocks: ")
                sb.append(mifareTag.blockCount)
            }

            if (tech == MifareUltralight::class.java.name) {
                sb.append('\n')
                val mifareUlTag = MifareUltralight.get(tag)
                var type = "Unknown"
                when (mifareUlTag.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> type = "Ultralight"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> type = "Ultralight C"
                }
                sb.append("Mifare Ultralight type: ")
                sb.append(type)
            }
        }
        Log.d("Datos: ", sb.toString())

        val TIME_FORMAT = SimpleDateFormat.getDateTimeInstance()
        val now = Date()

        textView!!.setText(TIME_FORMAT.format(now) + '\n'.toString() + sb.toString())
        return sb.toString()
    }


    private fun getHex(bytes: ByteArray): String {
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

    private fun getDec(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices) {
            val value = bytes[i] and 0xffL.toByte()
            result += value * factor
            factor *= 256L
        }
        return result
    }

    private fun getReversed(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices.reversed()) {
            val value = bytes[i] and 0xffL.toByte()
            result += value * factor
            factor *= 256L
        }
        return result
    }

    private fun ByteArrayToHexString(inarray: ByteArray): String {

        Log.d("ByteArrayToHexString", inarray.toString())

        var i: Int
        var j: Int
        var `in`: Int
        val hex =
            arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
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
