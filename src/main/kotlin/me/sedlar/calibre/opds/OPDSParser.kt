package me.sedlar.calibre.opds

import me.sedlar.calibre.helper.asOPDSEntry
import me.sedlar.calibre.helper.asOPDSSeriesEntry
import me.sedlar.calibre.helper.strAttr
import me.sedlar.calibre.helper.toArray
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibre.opds.local.OPDSSeries
import me.sedlar.calibre.opds.model.OPDSEntry
import org.w3c.dom.Document
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

class OPDSParser(
    private val baseURL: String,
    private val username: String,
    private val password: String,
    private val dataDir: File = File("./calibre-data")
) {

    /**
     * Parses the Calibre library into a list of OPDSLibrary
     *
     * @return The Calibre library as a list of OPDSLibrary
     */
    fun parse(): List<OPDSLibrary> {
        val list = ArrayList<OPDSLibrary>()

        handleCaching(
            data = OPDSConnector.readTextByDigest("$baseURL/opds", username, password),
            target = File(dataDir, "calibre_opds.xml")
        )?.let { rootXML ->
            list.addAll(parseLibraries(createXMLDoc(rootXML)))
        }

        return list
    }

    private fun parseLibraries(rootDoc: Document): List<OPDSLibrary> {
        val libs = ArrayList<OPDSLibrary>()

        rootDoc.getElementsByTagName("entry").toArray()
            .map { it.asOPDSEntry() }
            .filter { it.id.startsWith("calibre-library:") }
            .forEach { entry ->
                val library = OPDSLibrary(baseURL, username, password, dataDir, entry)

                parseLibrary(library)

                libs.add(library)
            }

        return libs
    }

    private fun parseLibrary(library: OPDSLibrary) {
        handleCaching(
            data = OPDSConnector.readTextByDigest("$baseURL/opds?library_id=${library.name}", username, password),
            target = File(dataDir, "libs/${library.name}/root.xml")
        )?.let { libXML ->
            val doc = createXMLDoc(libXML)

            doc.getElementsByTagName("entry").toArray()
                .map { it.asOPDSEntry() }
                .firstOrNull { it.title == "By Series" }
                ?.let { seriesEntry ->
                    parseSeriesList(library, seriesEntry)
                }
        }
    }

    private fun parseSeriesList(library: OPDSLibrary, seriesListEntry: OPDSEntry, offset: Int = 0, page: Int = 1) {
        handleCaching(
            data = OPDSConnector.readTextByDigest(
                baseURL + seriesListEntry.link + "&amp;offset=$offset",
                username,
                password
            ),
            target = File(dataDir, "libs/${library.name}/series-${page}.xml")
        )?.let { seriesListXML ->
            val doc = createXMLDoc(seriesListXML)

            doc.getElementsByTagName("entry").toArray()
                .map { it.asOPDSEntry() }
                .forEach { entry ->
                    val series = OPDSSeries(entry)

                    parseSeriesEntries(library, series)

                    library.seriesList.add(series)
                }

            doc.getElementsByTagName("link").toArray()
                .firstOrNull {
                    it.strAttr("type").contains("type=feed") && it.strAttr("type")
                        .contains("atom+xml") && it.strAttr("rel") == "next"
                }?.let { nextLink ->
                    val nextLinkHref = nextLink.strAttr("href")
                    if (nextLinkHref.contains("&offset=")) {
                        println("pog?")
                        val nextLinkOffset = nextLinkHref.split("&offset=")[1].toInt()
                        parseSeriesList(library, seriesListEntry, nextLinkOffset, page + 1)
                    }
                }
        }
    }

    private fun parseSeriesEntries(library: OPDSLibrary, series: OPDSSeries) {
        handleCaching(
            data = OPDSConnector.readTextByDigest(baseURL + series.link, username, password),
            target = File(dataDir, "libs/${library.name}/series-list/${series.pathName}.xml")
        )?.let { seriesXML ->
            val doc = createXMLDoc(seriesXML)

            doc.getElementsByTagName("entry").toArray()
                .map { it.asOPDSSeriesEntry() }
                .forEach { entry ->
                    series.entries.add(entry)
                }
        }
    }
}

private fun handleCaching(data: String?, target: File): String? {
    return if (data == null) {
        tryRead(target)
    } else {
        target.parentFile.mkdirs()
        Files.write(target.toPath(), data.toByteArray())
        data
    }
}

private fun tryRead(file: File, charset: Charset = Charsets.UTF_8): String {
    if (!file.exists()) {
        error("XML File does not exist")
    } else {
        return file.readText(charset)
    }
}

private fun createXMLDoc(data: String): Document {
    val builderFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = builderFactory.newDocumentBuilder()
    return docBuilder.parse(data.byteInputStream())
}