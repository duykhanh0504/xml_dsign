package com.moneyforward.nfc_dsxml

import android.content.Context
import android.util.Log
import nu.xom.canonical.Canonicalizer
import nu.xom.canonical.Canonicalizer.CANONICAL_XML
import java.io.*
import android.util.Base64
import nu.xom.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by Kan on 2020-05-31
 * Copyright © 2018 Money Forward, Inc. All rights
 */

class XmlUtils {
    companion object {

        private const val SIGNATURE = "dsig:Signature"

        private const val SIGNED_INFO = "dsig:SignedInfo"

        private const val CANONICAL_MEDTHOD = "dsig:CanonicalizationMethod"

        private const val ALGORITHM = "Algorithm"

        private const val SIGNATUREMEDTHOD = "dsig:SignatureMethod"

        private const val REFERENCE = "dsig:Reference"

        private const val TRANSFROMS = "dsig:Transforms"

        private const val TRANSFROM = "dsig:Transform"

        private const val DIGEST_MEDTHOD = "dsig:DigestMethod"

        private const val DIGEST_VALUE = "dsig:DigestValue"

        private const val SIGNATURE_VALUE = "dsig:SignatureValue"

        private const val KEY_INFO = "dsig:KeyInfo"
        private const val X509_DATA = "dsig:X509Data"
        private const val X509_CERTIFICATE = "dsig:X509Certificate"


        fun getXmltoByteArray(context: Context?, fileName: String): InputStream? {
            var xmlString: String? = null
            val am = context?.assets
            var stream: InputStream? = null
            try {
                stream = am?.open(fileName)
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            return stream
        }

        fun getXml(context: Context?, fileName: String): String? {
            var xmlString: String? = null
            val am = context?.assets
            try {
                val stream = am?.open(fileName)
                val length = stream?.available()
                val data = length?.let { ByteArray(it) }
                stream?.read(data)
                xmlString = data?.let { String(it) }
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            return xmlString
        }

        @Throws(IOException::class)
        fun read(context: Context?, file: String): ByteArray? {
            var ret: ByteArray? = null

            if (context != null) {
                try {
                    val inputStream = context!!.openFileInput(file)
                    val outputStream = ByteArrayOutputStream()

                    var nextByte = inputStream.read()
                    while (nextByte != -1) {
                        outputStream.write(nextByte)
                        nextByte = inputStream.read()
                    }

                    ret = outputStream.toByteArray()

                } catch (ignored: FileNotFoundException) {
                }

            }

            return ret
        }

        fun canonicalizerXml(byteArrayOutputStream: ByteArray): ByteArray {
            try {
                val stream = ByteArrayInputStream(byteArrayOutputStream)
                val parser = Builder()
                val doc = parser.build(stream)
                val canonicalOs = ByteArrayOutputStream()
                val canonicalizer = Canonicalizer(canonicalOs, CANONICAL_XML)
                canonicalizer.write(doc)
                return canonicalOs.toByteArray()
            } catch (e: Exception) {
                Log.e("error", e.message)
            }
            return ArrayList<Byte>().toByteArray()
        }

        fun toHex(arg: String): String {
            return String.format("%040x", BigInteger(1, arg.toByteArray()/*YOUR_CHARSET?*/))
        }

        @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
        fun SHA1(text: String): ByteArray {
            val md = MessageDigest.getInstance("SHA-1")
            val textBytes = text.toByteArray()
            md.update(textBytes, 0, textBytes.size)
            return md.digest()

        }

        fun canonicali(byteArray: ByteArray): String {
            val stream = ByteArrayInputStream(byteArray)
            val parser = Builder()
            val doc = parser.build(stream)
            val canonicalOs = ByteArrayOutputStream()
            val canonicalizer = Canonicalizer(canonicalOs, CANONICAL_XML)
            canonicalizer.write(doc)
            return canonicalOs.toString()
        }

        fun createDigistInfo(byteArrayOutputStream: ByteArray): ByteArray {
            try {
                val stream = ByteArrayInputStream(byteArrayOutputStream)
                val parser = Builder()
                val doc = parser.build(stream)
                val canonicalOs = ByteArrayOutputStream()
                val canonicalizer = Canonicalizer(canonicalOs, CANONICAL_XML)
                canonicalizer.write(doc)
                canonicalOs.toByteArray()
                val doc1 = parser.build(ByteArrayInputStream(canonicalOs.toByteArray()))
                var element = doc1.rootElement.childElements[0]
                element.namespaceURI = "http://xml.e-tax.nta.go.jp/XSD/kyotsu"
                element.addNamespaceDeclaration("gen", "http://xml.e-tax.nta.go.jp/XSD/general")
                element.addNamespaceDeclaration("kyo", "http://xml.e-tax.nta.go.jp/XSD/kyotsu")
                element.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance")
                element.toXML()
                //   var doc2 = Builder().build("")
                // doc2.rootElement = element
                var root = doc1.rootElement.childElements[0].toXML()
                root
                val sha1 = MessageDigest.getInstance("SHA1")
                val test2 =
                    "<PTE0010 xmlns=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:gen=\"http://xml.e-tax.nta.go.jp/XSD/general\" xmlns:kyo=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><CATALOG id=\"CATALOG\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:description id=\"REPORT\"><SEND_DATA /><IT_SEC><rdf:description about=\"#IT\" /></IT_SEC><FORM_SEC><rdf:Seq /></FORM_SEC><TENPU_SEC /><XBRL_SEC /><SOFUSHO_SEC /></rdf:description></rdf:RDF></CATALOG><CONTENTS id=\"CONTENTS\"><IT VR=\"1.2\" id=\"IT\"><ZEIMUSHO ID=\"ZEIMUSHO\"><gen:zeimusho_CD>09401</gen:zeimusho_CD><gen:zeimusho_NM>高知</gen:zeimusho_NM></ZEIMUSHO><NOZEISHA_ID ID=\"NOZEISHA_ID\">1503042913920076</NOZEISHA_ID><NOZEISHA_NM ID=\"NOZEISHA_NM\">国税太郎</NOZEISHA_NM><NOZEISHA_ADR ID=\"NOZEISHA_ADR\">高知県高知市神田２０００</NOZEISHA_ADR><TETSUZUKI ID=\"TETSUZUKI\"><procedure_CD>PTE0010</procedure_CD><procedure_NM>電子証明書の登録</procedure_NM></TETSUZUKI></IT></CONTENTS></PTE0010>"
                val test =
                    "<PTE0010 xmlns=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:gen=\"http://xml.e-tax.nta.go.jp/XSD/general\" xmlns:kyo=\"http://xml.e-tax.nta.go.jp/XSD/kyotsu\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" VR=\"1.0\" id=\"PTE0010\"><CATALOG id=\"CATALOG\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:description id=\"REPORT\"><SEND_DATA/><IT_SEC><rdf:description about=\"#IT\"/></IT_SEC><FORM_SEC><rdf:Seq/></FORM_SEC><TENPU_SEC/><XBRL_SEC/><SOFUSHO_SEC/></rdf:description></rdf:RDF></CATALOG><CONTENTS id=\"CONTENTS\"><IT VR=\"1.2\" id=\"IT\"><ZEIMUSHO ID=\"ZEIMUSHO\"><gen:zeimusho_CD>09401</gen:zeimusho_CD><gen:zeimusho_NM>高知</gen:zeimusho_NM></ZEIMUSHO><NOZEISHA_ID ID=\"NOZEISHA_ID\">1503042913920076</NOZEISHA_ID><NOZEISHA_NM ID=\"NOZEISHA_NM\">国税太郎</NOZEISHA_NM><NOZEISHA_ADR ID=\"NOZEISHA_ADR\">高知県高知市神田２０００</NOZEISHA_ADR><TETSUZUKI ID=\"TETSUZUKI\"><procedure_CD>PTE0010</procedure_CD><procedure_NM>電子証明書の登録</procedure_NM></TETSUZUKI></IT></CONTENTS></PTE0010>"
                sha1.reset()
                SHA1(test)
                toHex(test)
                Utils.toHex(
                    SHA1(
                        toHex(element.toXML())
                    )
                )
                return SHA1(
                    canonicali(
                        test.toByteArray()
                    )
                )
            } catch (e: Exception) {
                Log.e("error", e.message)
            }
            return ArrayList<Byte>().toByteArray()
        }


        fun createDigistInfoBase64(byteArrayOutputStream: ByteArray): String {
            try {
                val stream = ByteArrayInputStream(byteArrayOutputStream)
                val parser = Builder()
                val doc = parser.build(stream)
                val canonicalOs = ByteArrayOutputStream()
                val canonicalizer = Canonicalizer(canonicalOs, CANONICAL_XML)
                canonicalizer.write(doc)
                canonicalOs.toByteArray()
                val sha1 = MessageDigest.getInstance("SHA1")
                sha1.reset()
                sha1.update(java.nio.ByteBuffer.wrap(canonicalOs.toByteArray()))
                val salidasha1 = sha1.digest()
                val tagDigestValue = String(Base64.encode(salidasha1, Base64.NO_WRAP))
                return tagDigestValue
            } catch (e: Exception) {
                Log.e("error", e.message)
            }
            return ""
        }

        fun prettyXml(doc: Document): String {
            val out = ByteArrayOutputStream()
            val serializer = Serializer(out)
            serializer.indent = 2
            serializer.write(doc)
            return out.toString("UTF-8")
        }

        fun getIdRoot(xmlData: String): String {
            val builder = Builder()

            val inputStream = ByteArrayInputStream(xmlData.toByteArray(Charsets.UTF_8))

            var doc = builder.build(inputStream)
            var child = doc.rootElement

            return child.childElements[0].localName
        }


        fun toXML(doc: Document): String {

            val result = StringBuffer(64)

            result.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")

            // children
            for (i in 0 until doc.childCount) {
                result.append(doc.getChild(i).toXML())
                result.append("\n")
            }

            return result.toString()

        }

        fun getRawXMLToSignature(xmlData: String): String {
            val builder = Builder()

            val inputStream = ByteArrayInputStream(xmlData.toByteArray(Charsets.UTF_8))

            var doc = builder.build(inputStream)

            var child = doc.rootElement

            var newDoc = Element(child.childElements[0].localName, child.namespaceURI)
            for (i in 0 until child.namespaceDeclarationCount) {
                if (child.getNamespacePrefix(i).isNotEmpty()) {
                    newDoc.addNamespaceDeclaration(
                        child.getNamespacePrefix(i),
                        child.getNamespaceURI(child.getNamespacePrefix(i))
                    )
                }
            }

            for (i in 0 until child.childElements[0].attributeCount) {
                newDoc.addAttribute(
                    Attribute(
                        child.childElements[0].getAttribute(i).localName,
                        child.childElements[0].getAttribute(i).value
                    )
                )
            }

            for (i in 0 until child.childElements[0].childCount) {
                val subChild = child.childElements[0].getChild(i).copy()
                newDoc.appendChild(subChild)
            }

            return newDoc.toXML()
        }


        fun signatureXML(dataXml: String, element: Element): String {
            val builder = Builder()
            val inputStream = ByteArrayInputStream(dataXml.toByteArray(Charsets.UTF_8))

            var doc = builder.build(inputStream)
            try {
                val serializer = Serializer(System.out, "UTF-8")
                serializer.indent = 4
                serializer.maxLength = 64
                serializer.write(doc)
            } catch (ex: IOException) {
                System.err.println(ex)
            }

            var root = doc.rootElement
            root.insertChild(element, 1)

            return toXML(doc)

        }

        fun getXMLSignature(dataXml: InputStream, element: Element): String {
            val builder = Builder()

            var doc = builder.build(dataXml)
            var root = doc.rootElement
            root.insertChild(element, 1)

            return prettyXml(doc)

        }

        fun createXmlSignature(
            signInfo: String,
            signValue: String,
            certificate: String,
            tagIdURI: String
        ): Element {
            var signature = Element(SIGNATURE, "http://www.w3.org/2000/09/xmldsig#")
            try {
                var dignedInfo = Element(SIGNED_INFO, "http://www.w3.org/2000/09/xmldsig#")
                var canonicalMedthod =
                    Element(CANONICAL_MEDTHOD, "http://www.w3.org/2000/09/xmldsig#")
                val attAlgorthm = Attribute(ALGORITHM, "http://www.w3.org/2010/xml-c14n2")
                canonicalMedthod.addAttribute(attAlgorthm)
                val signatureMethod =
                    Element(SIGNATUREMEDTHOD, "http://www.w3.org/2000/09/xmldsig#")
                signatureMethod.addAttribute(
                    Attribute(
                        ALGORITHM,
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"
                    )
                )
                dignedInfo.insertChild(canonicalMedthod, 0)
                dignedInfo.insertChild(signatureMethod, 1)

                var reference = Element(REFERENCE, "http://www.w3.org/2000/09/xmldsig#")
                if (tagIdURI.isNotEmpty()) {
                    reference.addAttribute(Attribute("URI", "#$tagIdURI"))
                }
                var transfroms = Element(TRANSFROMS, "http://www.w3.org/2000/09/xmldsig#")
                var transfrom = Element(TRANSFROM, "http://www.w3.org/2000/09/xmldsig#")
                transfrom.addAttribute(
                    Attribute(
                        ALGORITHM,
                        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
                    )
                )
                transfroms.insertChild(transfrom, 0)
                reference.insertChild(transfroms, 0)

                var digestMedthod = Element(DIGEST_MEDTHOD, "http://www.w3.org/2000/09/xmldsig#")
                digestMedthod.addAttribute(
                    Attribute(
                        ALGORITHM,
                        "http://www.w3.org/2001/04/xmlenc#sha256"
                    )
                )

                reference.insertChild(digestMedthod, 1)

                var digistValue = Element(DIGEST_VALUE, "http://www.w3.org/2000/09/xmldsig#")
                digistValue.appendChild(signInfo)
                reference.insertChild(digistValue, 2)

                dignedInfo.insertChild(reference, 2)

                var sinatureValue = Element(SIGNATURE_VALUE, "http://www.w3.org/2000/09/xmldsig#")
                sinatureValue.appendChild(signValue)

                var keyInfo = Element(KEY_INFO, "http://www.w3.org/2000/09/xmldsig#")

                var x509Data = Element(X509_DATA, "http://www.w3.org/2000/09/xmldsig#")

                var x509Certificate =
                    Element(X509_CERTIFICATE, "http://www.w3.org/2000/09/xmldsig#")
                x509Certificate.appendChild(certificate)

                x509Data.insertChild(x509Certificate, 0)
                keyInfo.insertChild(x509Data, 0)


                signature.insertChild(dignedInfo, 0)
                signature.insertChild(sinatureValue, 1)
                signature.insertChild(keyInfo, 2)
            } catch (e: Exception) {
                Log.e("error", e.message)
            }
            return signature
        }

    }
}