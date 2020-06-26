package me.sedlar.calibre.opds

import org.apache.http.HttpStatus
import org.apache.http.auth.AUTH
import org.apache.http.auth.AuthenticationException
import org.apache.http.auth.MalformedChallengeException
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.auth.DigestScheme
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HttpContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.Charset

object OPDSConnector {

    var emulateOffline = false

    var checkTimeout = 5000
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

    fun readBytesByDigest(url: String, username: String, password: String): ByteArray? {
        if (emulateOffline || !checker(url)) {
            println("Connection does not exist, using local data.")
            return null
        }
        var bytes: ByteArray? = null
        val httpClient = HttpClientBuilder.create().build()
        val httpGet = HttpGet(url)
        val httpContext: HttpContext = BasicHttpContext()
        var httpResponse: CloseableHttpResponse? = null
        try {
            httpResponse = httpClient.execute(httpGet, httpContext)
            if (httpResponse.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                val authHeader = httpResponse?.getFirstHeader(AUTH.WWW_AUTH)
                val digestScheme = DigestScheme()

                /*
                override values if need
                No need override values such as nonce, opaque, they are generated by server side
                */
                digestScheme.overrideParamter("realm", "calibre")
                digestScheme.processChallenge(authHeader)
                val creds = UsernamePasswordCredentials(username, password)
                httpGet.addHeader(digestScheme.authenticate(creds, httpGet, httpContext))
                httpResponse?.close()
                httpResponse = httpClient.execute(httpGet)
            }
            bytes = httpResponse?.entity?.content?.readBytes()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: MalformedChallengeException) {
            e.printStackTrace()
        } catch (e: AuthenticationException) {
            e.printStackTrace()
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return bytes
    }

    fun readTextByDigest(url: String, username: String, password: String, charset: Charset = Charsets.UTF_8): String? {
        readBytesByDigest(url, username, password)?.let {
            return String(it, charset)
        }

        return null
    }
}