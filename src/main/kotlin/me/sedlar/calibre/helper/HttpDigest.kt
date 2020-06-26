package me.sedlar.calibre.helper

import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

private val UCDIGITS =
    charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

class HttpDigest(private val user: String, private val pass: String, wwwAuth: String) {

    private val nonce: String?
    private val realm: String?
    private val opaque: String?
    private val qop: String?

    private var nonceCount = 1

    init {
        val args = getArgs(wwwAuth)
        nonce = args["nonce"]
        realm = args["Digest realm"]
        opaque = args["opaque"]
        qop = args["qop"]
    }

    private fun getArgs(wwwAuth: String): Map<String, String> {
        val args: MutableMap<String, String> = HashMap()
        val split = wwwAuth.split(", ").toTypedArray()
        for (part in split) {
            val pair = part.split("=").toTypedArray()
            args[pair[0]] = pair[1].substring(1, pair[1].length - 1)
        }
        return args
    }

    fun calculateDigestAuthorization(method: String, digestURI: String): String {
        val cnonce = java.lang.Long.toHexString(java.lang.Double.doubleToLongBits(Math.random()))
        return calculateDigestAuthorization(method, digestURI, cnonce)
    }

    fun getISOHash(s: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(s.toByteArray(Charset.forName("ISO-8859-1")))
        return encodeHex(md.digest())
    }

    fun calculateDigestAuthorization(
        method: String,
        digestURI: String,
        cnonce: String
    ): String {
        val ha1 = getISOHash("$user:$realm:$pass").toLowerCase()
        val ha2 = getISOHash("$method:$digestURI").toLowerCase()
        val nonceCountStr = String.format("%08d", nonceCount++)
        val response =
            getISOHash("$ha1:$nonce:$nonceCountStr:$cnonce:$qop:$ha2").toLowerCase()
        return "Digest username=\"$user\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$digestURI\", qop=auth, nc=$nonceCountStr, cnonce=\"$cnonce\", response=\"$response\", opaque=\"$opaque\""
    }

    private fun encodeHex(data: ByteArray, DIGITS: CharArray = UCDIGITS): String {
        val l = data.size
        val out = CharArray(l shl 1)

        // two characters form the hex value.
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = DIGITS[0xF0 and data[i].toInt() ushr 4]
            out[j++] = DIGITS[0x0F and data[i].toInt()]
            i++
        }
        return String(out)
    }
}