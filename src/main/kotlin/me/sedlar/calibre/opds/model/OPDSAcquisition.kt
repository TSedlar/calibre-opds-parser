package me.sedlar.calibre.opds.model

import java.io.Serializable

data class OPDSAcquisition(
    val type: String,
    val link: String,
    val length: Long,
    val mtime: String
) : Serializable {

    val fileExtension: String
        get() {
            return try {
                val ext = link.substring(link.indexOf("/get/") + 5)
                ext.substring(0, ext.indexOf("/"))
            } catch (err: Throwable) {
                when (type) {
                    "application/epub+zip" -> "epub"
                    else -> "zip"
                }
            }
        }
}