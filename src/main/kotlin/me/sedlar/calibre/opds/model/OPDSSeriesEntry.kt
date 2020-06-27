package me.sedlar.calibre.opds.model

import java.io.Serializable

data class OPDSSeriesEntry(
    val title: String,
    val authorName: String,
    val id: String,
    val updated: String,
    val published: String,
    val acquisitions: List<OPDSAcquisition>,
    val cover: String,
    val thumbnail: String
): Serializable {

    val uuid = id.replace("urn:uuid:", "")
}