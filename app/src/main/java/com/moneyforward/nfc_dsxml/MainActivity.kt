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
import android.nfc.tech.NfcF
import android.os.Build
import android.util.Base64
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.moneyforward.nfc_dsxml.common.showAlertWithPositive
import com.moneyforward.nfc_dsxml.extension.hideKeyboard
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.tvContent
import kotlinx.android.synthetic.main.modal_bottomsheet.view.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    var dialog: BottomSheetDialog? = null
    var xmlRawData = ""
    var xmlRawDigisInfo = ""
    var shaDigisInfo: ByteArray = byteArrayOf()
    var digistValue = ""
    var IdRoot = ""
    var certificate = ""
    var digitalSinatureValue = ""
    var xmlAfterSign = ""

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

    private fun updateButton() {
        btnShowXml.isEnabled = xmlAfterSign.isNotEmpty() && xmlRawData.isNotEmpty()
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
            tvDigistInfo.text = digistValue
        }

    }

    private fun handleError(data: ByteArray, title: String = "error", msg: String): Boolean {
        if (Utils.toHex(data) != NFCStatus.SUCCESS.value) {
            when {
                Utils.toHex(data) == NFCStatus.FILE_NOT_FOUND.value -> showAlertWithPositive(
                    title,
                    "$msg: file not found ${Utils.toHex(data)}"
                )
                Utils.toHex(data) == NFCStatus.NOT_ALLOW.value -> showAlertWithPositive(
                    title,
                    "$msg: not allow command ${Utils.toHex(data)}"
                )
                Utils.toHex(data) == NFCStatus.SECURITY_STATUS.value -> showAlertWithPositive(
                    title,
                    "$msg: Security status not satisfied ${Utils.toHex(data)}"
                )
                else -> showAlertWithPositive(
                    title,
                    "$msg: error code ${Utils.toHex(data)}"
                )
            }
            return false
        }
        return true
    }

    private fun connectCard(isodep: IsoDep): Boolean {
        var result = false

        if (!isodep.isConnected) {
            try {
                isodep.connect()
                result = true
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
                Log.v("error", e.toString())
            }

        }
        return result
    }


    private fun readNFCTag(nfcTag: Tag?): String {
        var result = ""
        nfcTag?.also {
            try {
                val isoDep = IsoDep.get(it)

                if (isoDep == null) {
                    showAlertWithPositive(
                        "error",
                        "don't support tag  , please try again"
                    )
                    return@also
                }
                if (!connectCard(isoDep)) {
                    showAlertWithPositive(
                        "error",
                        "not support card  , please try again"
                    )
                    return@also
                }

                /////////
                // generate digital signature
                //////////

                val readFileToGetSign = isoDep.transceive(
                    NfcUtils.commandSelectFile
                )

                if (!handleError(readFileToGetSign, msg = "step 2- 1")) {
                    isoDep.close()
                    return@also
                }

                val selectFilePin = isoDep.transceive(
                    NfcUtils.commandSelectFilePin
                )

                if (!handleError(selectFilePin, msg = "step 2- 2")) {
                    isoDep.close()
                    return@also
                }

                val commandVerifyPin =
                    NfcUtils.commandSignaturePin(inputPin.text.toString().toByteArray())
                val verifyPin = isoDep.transceive(
                    commandVerifyPin
                )
                if (Utils.toHex(verifyPin) != NFCStatus.SUCCESS.value) {
                    if (Utils.toHex(verifyPin) == NFCStatus.PIN_LOCK.value) {
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
                if (!handleError(selectComputerDigitalFile, msg = "step 2- 4")) {
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
                    if (Utils.toHex(status) != NFCStatus.SUCCESS.value) {
                        showAlertWithPositive(
                            "error",
                            "step 2 -5 : unknow error ${Utils.toHex(status)}"
                        )
                        isoDep.close()
                        return@also

                    } else {
                        val dataSignature = singatureData.copyOfRange(0, singatureData.size - 2)
                        digitalSinatureValue = String(Base64.encode(dataSignature, Base64.NO_WRAP))
                    }
                }

                /////////
                // read certificate
                //////////

                val readFileGetCertificate = isoDep.transceive(
                    NfcUtils.commandSelectFile
                )
                if (!handleError(readFileGetCertificate, msg = "step 1 - 1")) {
                    isoDep.close()
                    return@also
                }

                val selectCertificate = isoDep.transceive(
                    NfcUtils.commandSelectCertificate
                )

                if (!handleError(selectCertificate, msg = "step 1 - 2")) {
                    isoDep.close()
                    return@also
                }

                val readCertificate = NfcUtils.ReadData(isoDep)

                if (readCertificate.size <= 2) {
                    showAlertWithPositive(
                        "error",
                        "step 1 - 3: can not read certificate ${Utils.toHex(readCertificate)}"
                    )
                    isoDep.close()
                    return@also
                } else {
                    certificate = String(
                        Base64.encode(
                            readCertificate,
                            Base64.NO_WRAP
                        )
                    )
                }

                isoDep.close()

                result = XmlUtils.signatureXML(
                    xmlRawData, XmlUtils.createXmlSignature(
                        signInfo = digistValue, signValue = digitalSinatureValue,
                        certificate = certificate, tagIdURI = IdRoot
                    )
                )
            } catch (e: Exception) {
                e.message?.let { it1 ->
                    showAlertWithPositive(
                        "error", it1
                    )
                }
            }
        }
        dialog?.dismiss()
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

        btnShowXml.setOnClickListener {
            val intent = SignXmlDetailActivity.createIntent(this, xmlRawData, xmlAfterSign)
            this.startActivity(intent)
        }

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
                "NFC Tag\n" + Utils.ByteArrayToHexString(
                    intent.getByteArrayExtra(
                        NfcAdapter.EXTRA_ID
                    )!!
                )
            )

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            xmlAfterSign = readNFCTag(tag)
            if (xmlAfterSign.isNotEmpty()) {
                showAlertWithPositive(
                    "Success",
                    "Sign xml successfully"
                )
            }
            updateButton()
        }
    }

}