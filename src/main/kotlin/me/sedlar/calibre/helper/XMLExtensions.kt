package me.sedlar.calibre.helper

import me.sedlar.calibre.opds.model.OPDSAcquisition
import me.sedlar.calibre.opds.model.OPDSEntry
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import org.w3c.dom.Node
import org.w3c.dom.NodeList

private const val SPEC_ACQUISITION = "http://opds-spec.org/acquisition"
private const val SPEC_COVER = "http://opds-spec.org/cover"
private const val SPEC_THUMBNAIL = "http://opds-spec.org/thumbnail"

fun NodeList.toArray(): Array<Node> {
    return Array(this.length) {
        this.item(it)
    }
}

fun Node.firstByTag(tag: String): Node {
    return this.childNodes.toArray().first { it.nodeName == tag }
}

fun Node.textAtTag(tag: String): String {
    return firstByTag(tag).textContent
}

fun Node.strAttr(key: String): String {
    return attributes.getNamedItem(key).textContent
}

fun Node.asOPDSEntry(): OPDSEntry {
    return OPDSEntry(
        title = textAtTag("title"),
        id = textAtTag("id"),
        updated = textAtTag("updated"),
        content = textAtTag("content"),
        link = firstByTag("link").attributes.getNamedItem("href").textContent
    )
}

fun Node.asOPDSSeriesEntry(): OPDSSeriesEntry {
    return OPDSSeriesEntry(
        title = textAtTag("title"),
        authorName = firstByTag("author").textAtTag("name"),
        id = textAtTag("id"),
        updated = textAtTag("updated"),
        published = textAtTag("published"),
        acquisitions = childNodes.toArray()
            .filter { it.nodeName == "link" && it.strAttr("rel") == SPEC_ACQUISITION }
            .map { it.asOPDSAcquisition() },
        cover = childNodes.toArray()
            .first { it.nodeName == "link" && it.strAttr("rel") == SPEC_COVER }
            .strAttr("href"),
        thumbnail = childNodes.toArray()
            .first { it.nodeName == "link" && it.strAttr("rel") == SPEC_THUMBNAIL }
            .strAttr("href")
    )
}

fun Node.asOPDSAcquisition(): OPDSAcquisition {
    return OPDSAcquisition(
        type = strAttr("type"),
        link = strAttr("href"),
        length = strAttr("length").toLong(),
        mtime = strAttr("mtime")
    )
}