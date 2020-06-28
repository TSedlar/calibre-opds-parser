package me.sedlar.calibre.opds.model

import java.io.Serializable

data class OPDSAcquisition(
    val type: String,
    val link: String,
    val length: Long,
    val mtime: String
) : Serializable {

    val fileExtension = when (type) {
        "application/epub+zip" -> "epub"
        else -> "zip"
    }
}