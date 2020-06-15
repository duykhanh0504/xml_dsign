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
    var digitalSinatureValue = ""

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

    }

    private fun readNFCTag(nfcTag: Tag?): String {
        var result = ""
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
                    certificate = String(
                        Base64.encode(
                            readCertificate,
                            Base64.NO_WRAP
                        )
                    ) //String(readCertificate, Charsets.UTF_8)
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


                val digistInfo = NfcUtils.asn1DigestInfo(shaDigisInfo)

                val computerSignature =
                    NfcUtils.commandSignatureData(
                        digistInfo
                    )

                val singatureData = isoDep.transceive(computerSignature)

                if (singatureData.size < 2) {
                    showAlertWithPositive(
                        "error",
                        "step 2 -5 : unknow error"
                    )
                    isoDep.close()
                    return@also
                } else {
                    val status = byteArrayOf(
                        singatureData[singatureData.size - 2],
                        singatureData[singatureData.size - 1]
                    )
                    if (status != NFCStatus.SUCCESS.value) {
                        showAlertWithPositive(
                            "error",
                            "step 2 -5 : unknow error ${String(
                                status,
                                Charsets.UTF_8
                            )}"
                        )
                        isoDep.close()
                        return@also

                    } else {
                        val dataSignature = singatureData.copyOfRange(0, singatureData.size - 2)
                        digitalSinatureValue = String(Base64.encode(dataSignature, Base64.NO_WRAP))
                    }
                }
                isoDep.close()
                val signature = XmlUtils.createXmlSignature(
                    signInfo = digistValue, signValue = digitalSinatureValue,
                    certificate = certificate, tagIdURI = IdRoot
                )
                result = XmlUtils.signatureXML(xmlRawData, signature)

            } catch (e: Exception) {
                e.message?.let { it1 ->
                    showAlertWithPositive(
                        "error", it1
                    )
                }
            }
        }
        return result
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

            // test
             xmlRawData = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                     "<DATA xmlns=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:gen=\"http://xml.e-tax.nta.go.jp/XSD/general\" xmlns:kyo=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" id=\"DATA\"><PTE0010 VR=\"1.0\" id=\"PTE0010\"><CATALOG id=\"CATALOG\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:description id=\"REPORT\"><SEND_DATA/><IT_SEC><rdf:description about=\"#IT\"/></IT_SEC><FORM_SEC><rdf:Seq/></FORM_SEC><TENPU_SEC/><XBRL_SEC/><SOFUSHO_SEC/></rdf:description></rdf:RDF></CATALOG><CONTENTS id=\"CONTENTS\"><IT VR=\"1.2\" id=\"IT\"><ZEIMUSHO ID=\"ZEIMUSHO\"><gen:zeimusho_CD>09401</gen:zeimusho_CD><gen:zeimusho_NM>高知</gen:zeimusho_NM></ZEIMUSHO><NOZEISHA_ID ID=\"NOZEISHA_ID\">1503042913920076</NOZEISHA_ID><NOZEISHA_NM ID=\"NOZEISHA_NM\">国税太郎</NOZEISHA_NM><NOZEISHA_ADR ID=\"NOZEISHA_ADR\">高知県高知市神田２０００</NOZEISHA_ADR><TETSUZUKI ID=\"TETSUZUKI\"><procedure_CD>PTE0010</procedure_CD><procedure_NM>電子証明書の登録</procedure_NM></TETSUZUKI></IT></CONTENTS></PTE0010></DATA>\n"
             xmlRawDigisInfo = "<PTE0010 xmlns=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:gen=\"http://xml.e-tax.nta.go.jp/XSD/general\" xmlns:kyo=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" VR=\"1.0\" id=\"PTE0010\"><CATALOG id=\"CATALOG\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:description id=\"REPORT\"><SEND_DATA /><IT_SEC><rdf:description about=\"#IT\" /></IT_SEC><FORM_SEC><rdf:Seq /></FORM_SEC><TENPU_SEC /><XBRL_SEC /><SOFUSHO_SEC /></rdf:description></rdf:RDF></CATALOG><CONTENTS id=\"CONTENTS\"><IT VR=\"1.2\" id=\"IT\"><ZEIMUSHO ID=\"ZEIMUSHO\"><gen:zeimusho_CD>09401</gen:zeimusho_CD><gen:zeimusho_NM>高知</gen:zeimusho_NM></ZEIMUSHO><NOZEISHA_ID ID=\"NOZEISHA_ID\">1503042913920076</NOZEISHA_ID><NOZEISHA_NM ID=\"NOZEISHA_NM\">国税太郎</NOZEISHA_NM><NOZEISHA_ADR ID=\"NOZEISHA_ADR\">高知県高知市神田２０００</NOZEISHA_ADR><TETSUZUKI ID=\"TETSUZUKI\"><procedure_CD>PTE0010</procedure_CD><procedure_NM>電子証明書の登録</procedure_NM></TETSUZUKI></IT></CONTENTS></PTE0010>"
             digistValue = "EG5aEmJMEjWOaN7xHpKkE31VZEA="
             IdRoot = "PTE0010"
             certificate = "MIIDHTCCAgWgAwIBAgICBCswDQYJKoZIhvcNAQEFBQAwZTELMAkGA1UEBhMCSlAx\n" +
                     "HDAaBgNVBAoTE0phcGFuZXNlIEdvdmVybm1lbnQxHDAaBgNVBAsTE05hdGlvbmFs\n" +
                     "IFRheCBBZ2VuY3kxGjAYBgNVBAMTEVRFU1QgQ0EgZm9yIGUtVGF4MB4XDTEzMDMy\n" +
                     "MTEzMDMwNVoXDTI5MTIyOTAwMDAwMFowYzELMAkGA1UEBhMCSlAxHDAaBgNVBAoM\n" +
                     "E0phcGFuZXNlIEdvdmVybm1lbnQxHDAaBgNVBAsME05hdGlvbmFsIFRheCBBZ2Vu\n" +
                     "Y3kxGDAWBgNVBAMMD2NsaWNlcnQwMDAwMTA2NzCBnzANBgkqhkiG9w0BAQEFAAOB\n" +
                     "jQAwgYkCgYEAshUZlOMOBylulSIz7PIhcJ5ysl8+mS46nxyS/Y9jnkXzt0m4WSVF\n" +
                     "UsiJxNN8pMgHB2W20LNhAJV7Yg/+xmFQTHJzPDMvFOhlu2kmpqFwvcJfN5eof/00\n" +
                     "30a81Fe+l+8Vgqe5NjLAGZXPGBRCYpSsJVc1ub1G8Gnewddi7iaWH/8CAwEAAaNd\n" +
                     "MFswDAYDVR0TAQH/BAIwADALBgNVHQ8EBAMCBPAwHwYDVR0jBBgwFoAUQa/Xix2y\n" +
                     "a/drtwpfq3WBmidyPZEwHQYDVR0OBBYEFHyBYRMq7i4ySXaAm5Qoh7uNLD1UMA0G\n" +
                     "CSqGSIb3DQEBBQUAA4IBAQAG+wtIGVeg1KJH5PuKzQQRf9hWKhTw2d4jIUI/Mhok\n" +
                     "2gD7djn2iOZSS/qh3E8tJ4rgFUMehsYu3jLrWTL+HyuxPleO7F7yF8ubUPe62qLH\n" +
                     "kHt+sVUaMP3+Tt1VTO7dWJn6fp+fbBm4I8yi1G/VNokqtC1lezyH3fJMa42vCOI1\n" +
                     "hpU/xSq4Bg3zQYgJ8E/KK+X/0vpIYE80/E2jw5u3kUueViUz4EHXRcX6kdCzCR8T\n" +
                     "rOGpvykNjEKBN+A/6OSBMPIrLj1OL4+38VkmhEcl6xS+oZDIyeeHAkgPvbAoNFhO\n" +
                     "CKfpwGl/fg9ZCqjRbtJEs28GirzbvJzkgt10/TwT7dE/"
             digitalSinatureValue = "cuFFxOdV8uoPNQ7PdTyslkd4+uSCoU0kZUil+/S//xvwrZDa0WJXFBKH9cnPhryj\n" +
                     "HkzqrwjK+0y4alvKSpAiksOo2pgSEh+iBAR+fdIIicTOw3u0tealVf8WFHMCcaMn\n" +
                     "bPNprjCqsjYLUkEZkygdPwVEChRyp/Z6tbYEZCar4cA="

            val signature = XmlUtils.createXmlSignature(
                signInfo = digistValue, signValue = digitalSinatureValue,
                certificate = certificate, tagIdURI = IdRoot
            )
            val result = XmlUtils.signatureXML(xmlRawData, XmlUtils.createXmlSignature(
                signInfo = digistValue, signValue = digitalSinatureValue,
                certificate = certificate, tagIdURI = IdRoot
            ))
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
