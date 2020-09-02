package me.sedlar.calibre.opds

import me.sedlar.calibre.helper.HttpDigest
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import java.nio.charset.Charset


object OPDSConnector {

    var emulateOffline = false

    var checkTimeout = 10000
    var checker: (url: String) -> Boolean = { url ->
        val host = url
            .replace("http://", "")
            .replace("https://", "")
            .split(":")[0]

        var port = url.substring(url.lastIndexOf(':') + 1)
        if (port.contains('/')) {
            port = port.substring(0, port.indexOf('/'))
        }

        InetSocketAddress(InetAddress.getByName(host), port.toInt()).address.isReachable(checkTimeout)
    }

    /**
     * Retrieves the data at the given URL
     *
     * @param url The URL of the data to retrieve
     * @param username The username of your Calibre OPDS account
     * @param password The password of your Calibre OPDS account
     *
     * @return The data at the given URL
     */
    fun readBytesByDigest(url: String, username: String, password: String): ByteArray? {
        if (emulateOffline || !checker(url)) {
            println("Connection does not exist, using local data.")
            return null
        }

        val urlObj = URL(url)
        var connection: HttpURLConnection = urlObj.openConnection() as HttpURLConnection

        if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            val auth = connection.getHeaderField("WWW-Authenticate")

            val digest = HttpDigest(username, password, auth)
            val authHeader = digest.calculateDigestAuthorization("GET", connection.url.path)

            connection = urlObj.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", authHeader)
        }

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use { input ->
                return input.readBytes()
            }
        } else {
            error("Failed to authenticate -> ${connection.responseCode}: ${connection.responseMessage}")
        }
    }

    /**
     * Retrieves the data at the given URL formatted as text in the supplied charset
     *
     * @param url The URL of the data to retrieve
     * @param username The username of your Calibre OPDS account
     * @param password The password of your Calibre OPDS account
     * @param charset The charset for the data to be formatted as, UTF8 is the default.
     *
     * @return The data at the given URL formatted as text in the supplied charset
     */
    fun readTextByDigest(url: String, username: String, password: String, charset: Charset = Charsets.UTF_8): String? {
        readBytesByDigest(url, username, password)?.let {
            return String(it, charset)
        }

        return null
    }
}