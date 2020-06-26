package me.sedlar.calibre.opds.model

data class OPDSAcquisition(
    val type: String,
    val link: String,
    val length: Long,
    val mtime: String
)