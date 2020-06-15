package com.moneyforward.nfc_dsxml

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.lang.Exception
import java.math.BigInteger
import kotlin.experimental.and
import android.nfc.tech.NfcF
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.moneyforward.nfc_dsxml.common.showAlertWithPositive
import com.moneyforward.nfc_dsxml.extension.hideKeyboard
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.modal_bottomsheet.view.*

class MainActivity : AppCompatActivity() {

    var dialog: BottomSheetDialog? = null
    var xmlRawData = ""
    var xmlRawDigisInfo = ""
    var shaDigisInfo: ByteArray = byteArrayOf()
    var digistValue = ""
    var IdRoot = ""
    var certificate = ""

    private var animation: AnimatedVectorDrawable = AnimatedVectorDrawable()

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
        setContentView(R.layout.activity_main)
        requestPermission()
        initDialog()
        setListener()
        XmlReader()

    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, list.toTypedArray(), 23)
    }

    private fun XmlReader() {

        xmlRawData = XmlUtils.getXml(this, "xml/example1.xtx") ?: ""
        xmlRawDigisInfo = XmlUtils.getRawXMLToSignature(xmlRawData)
        IdRoot = XmlUtils.getIdRoot(xmlRawData)
        xmlRawDigisInfo.toByteArray().let {
            shaDigisInfo = XmlUtils.canonicalizerXml(it)
            digistValue = XmlUtils.createDigistInfoBase64(shaDigisInfo)
        }


        var xml = ""
        XmlUtils.getXmltoByteArray(this, "xml/example1.xtx")?.let {
            xml = XmlUtils.getXMLSignature(
                it,
                XmlUtils.createXmlSignature("", "", "")
            )
        }

        webView.loadData(xml, "text/html", null)

        val s = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        Utils.writeFileOnInternalStorage(
            s,
            "example_sign.xml",
            xml
        )
        var digistInfo: ByteArray = byteArrayOf()
        var digestValue = ""

    }

    private fun readNFCTag(nfcTag: Tag?) {
        nfcTag?.also {
            try {
                val isoDep = IsoDep.get(it)
                isoDep.connect()

                val readFileGetCertificate = isoDep.transceive(
                    NfcUtils.commandSelectFile
                )
                if (readFileGetCertificate != NFCStatus.SUCCESS.value) {
                    if (readFileGetCertificate == NFCStatus.FILE_NOT_FOUND.value)
                        showAlertWithPositive(
                            "error",
                            "step 1 : file not found ${String(
                                readFileGetCertificate,
                                Charsets.UTF_8
                            )}"
                        )
                    else if (readFileGetCertificate == NFCStatus.NOT_ALLOW.value)
                        showAlertWithPositive(
                            "error",
                            "step 1: not allow command ${String(
                                readFileGetCertificate,
                                Charsets.UTF_8
                            )}"
                        )
                    else {
                        showAlertWithPositive(
                            "error",
                            "step 1: error code ${String(readFileGetCertificate, Charsets.UTF_8)}"
                        )
                    }
                    isoDep.close()
                    return@also
                }

                val selectCertificate = isoDep.transceive(
                    NfcUtils.commandSelectCertificate
                )

                if (selectCertificate != NFCStatus.SUCCESS.value) {
                    if (selectCertificate == NFCStatus.FILE_NOT_FOUND.value)
                        showAlertWithPositive(
                            "error",
                            "step 2 : file not found ${String(
                                selectCertificate,
                                Charsets.UTF_8
                            )}"
                        )
                    else if (selectCertificate == NFCStatus.NOT_ALLOW.value)
                        showAlertWithPositive(
                            "error",
                            "step 2: not allow command ${String(selectCertificate, Charsets.UTF_8)}"
                        )
                    else {
                        showAlertWithPositive(
                            "error",
                            "step 2: error code ${String(selectCertificate, Charsets.UTF_8)}"
                        )
                    }
                    isoDep.close()
                    return@also
                }

                val readCertificate = NfcUtils.ReadData(isoDep)

                if (readCertificate.size <= 2) {
                    showAlertWithPositive(
                        "error",
                        "step 3: can not read certificate ${String(
                            readCertificate,
                            Charsets.UTF_8
                        )}"
                    )
                    isoDep.close()
                    return@also
                } else {
                    certificate = String(readCertificate, Charsets.UTF_8)
                }

                val readFileToGetSign = isoDep.transceive(
                    NfcUtils.commandSelectFile
                )

                if (!readFileToGetSign.contentEquals(NFCStatus.SUCCESS.value)) {
                    if (readFileToGetSign.contentEquals(NFCStatus.FILE_NOT_FOUND.value))
                        showAlertWithPositive(
                            "error",
                            "step 2 -1 : file not found ${String(
                                readFileToGetSign,
                                Charsets.UTF_8
                            )}"
                        )
                    else if (readFileGetCertificate.contentEquals(NFCStatus.NOT_ALLOW.value))
                        showAlertWithPositive(
                            "error",
                            "step 2-1: not allow command ${String(
                                readFileToGetSign,
                                Charsets.UTF_8
                            )}"
                        )
                    else {
                        showAlertWithPositive(
                            "error",
                            "step 2-1: error code ${String(readFileToGetSign, Charsets.UTF_8)}"
                        )
                    }
                    isoDep.close()
                    return@also
                }

                val selectFilePin = isoDep.transceive(
                    NfcUtils.commandSelectFilePin
                )
                if (selectFilePin != NFCStatus.SUCCESS.value) {
                    if (selectFilePin == NFCStatus.FILE_NOT_FOUND.value)
                        showAlertWithPositive(
                            "error",
                            "step 2 -2 : file not found ${String(
                                selectFilePin,
                                Charsets.UTF_8
                            )}"
                        )
                    else if (selectFilePin == NFCStatus.NOT_ALLOW.value)
                        showAlertWithPositive(
                            "error",
                            "step 2-2: not allow command ${String(selectFilePin, Charsets.UTF_8)}"
                        )
                    else {
                        showAlertWithPositive(
                            "error",
                            "step 2-2: error code ${String(selectFilePin, Charsets.UTF_8)}"
                        )
                    }
                    isoDep.close()
                    return@also
                }

                val commandVerifyPin =
                    NfcUtils.commandSignaturePin(inputPin.text.toString().toByteArray())
                val verifyPin = isoDep.transceive(
                    commandVerifyPin
                )
                if (verifyPin != NFCStatus.SUCCESS.value) {
                    if (verifyPin == NFCStatus.PIN_LOCK.value) {
                        showAlertWithPositive(
                            "error",
                            "step 2-3: pin lock  ${String(verifyPin, Charsets.UTF_8)}"
                        )
                    } else {
                        showAlertWithPositive(
                            "error",
                            "step 2-3: pin lock  ${String(verifyPin, Charsets.UTF_8)}"
                        )
                    }
                    isoDep.close()
                    return@also
                }

                val selectComputerDigitalFile = isoDep.transceive(
                    NfcUtils.commandSelectComputerDigitalFile
                )
                if (selectComputerDigitalFile != NFCStatus.SUCCESS.value) {
                    if (selectComputerDigitalFile == NFCStatus.FILE_NOT_FOUND.value)
                        showAlertWithPositive(
                            "error",
                            "step 2 -4 : file not found ${String(
                                selectFilePin,
                                Charsets.UTF_8
                            )}"
                        )
                    else if (selectComputerDigitalFile == NFCStatus.NOT_ALLOW.value)
                        showAlertWithPositive(
                            "error",
                            "step 2-4: not allow command ${String(
                                selectComputerDigitalFile,
                                Charsets.UTF_8
                            )}"
                        )
                    else {
                        showAlertWithPositive(
                            "error",
                            "step 2-4: error code ${String(
                                selectComputerDigitalFile,
                                Charsets.UTF_8
                            )}"
                        )
                    }
                    isoDep.close()
                    return@also
                }


                val computerSignature =
                    NfcUtils.commandSignatureData(
                        shaDigisInfo
                    )

                val response = isoDep.transceive(computerSignature)

                tvContent.text = "Card Response: " + Utils.toHex(
                    response
                )

                isoDep.close()

                //  isoDep.close()
            } catch (e: Exception) {
                Log.d("error", e.message)
            }
        }
    }

    private fun startNFC() {

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0
        )
        val filter = IntentFilter()
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED)
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
        // enabling foreground dispatch for getting intent from NFC event:
        NfcAdapter.getDefaultAdapter(this)
            ?.enableForegroundDispatch(this, pendingIntent, arrayOf(filter), this.techList)
    }

    private fun stopNFC() {
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
    }


    private fun setListener() {
        inputGroup.setOnClickListener {
            it.clearFocus()
        }

        inputPin.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                v.hideKeyboard()
            }
        }

        btnSendCommand.setOnClickListener {
            if (inputPin.text.isNullOrEmpty()) {
                showAlertWithPositive("error", "Please input Pin")
            } else {
                dialog?.show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    animation.reset()
                }
                animation.start()
                startNFC()
            }
        }

        dialog?.setOnDismissListener {
            stopNFC()
        }
    }

    private fun initDialog() {
        val modalBottomSheet = layoutInflater.inflate(R.layout.modal_bottomsheet, null)

        dialog = BottomSheetDialog(this)
        dialog?.also {
            it.setContentView(modalBottomSheet)
            it.setCanceledOnTouchOutside(true)
            it.setCancelable(true)
            val d: Drawable = modalBottomSheet.iconNFC.drawable
            if (d is AnimatedVectorDrawable) {
                animation = d
                animation.start()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d("onResume", "1")
    }

    override fun onPause() {
        super.onPause()
        stopNFC()
        Log.d("onPause", "1")

    }

    @SuppressLint("SetTextI18n")
    override fun onNewIntent(intent: Intent) {
        Log.d("onNewIntent", "1")

        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            Log.d("onNewIntent", "2")
            tvContent!!.setText(
                "NFC Tag\n" + ByteArrayToHexString(
                    intent.getByteArrayExtra(
                        NfcAdapter.EXTRA_ID
                    )!!
                )
            )

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            readNFCTag(tag)
        }
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
