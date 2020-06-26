package me.sedlar.calibre.opds.model

data class OPDSEntry(
    val title: String,
    val id: String,
    val updated: String,
    val content: String,
    val link: String
)