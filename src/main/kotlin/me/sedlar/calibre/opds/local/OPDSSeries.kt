package me.sedlar.calibre.opds.local

import me.sedlar.calibre.opds.model.OPDSEntry
import me.sedlar.calibre.opds.model.OPDSSeriesEntry

class OPDSSeries(entry: OPDSEntry) {

    val link = entry.link
    val name = entry.id.replace("calibre:category:", "")

    val entries = ArrayList<OPDSSeriesEntry>()
}