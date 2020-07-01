package me.sedlar.calibre.opds.local

import me.sedlar.calibre.opds.model.OPDSEntry
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import java.io.Serializable

class OPDSSeries(entry: OPDSEntry): Serializable {

    val link = entry.link
    val name = entry.id.replace("calibre:category:", "")
    val pathName = name
        .replace(":", " -")
        .replace("  ", " ")
        .replace(Regex("[^a-zA-Z0-9.\\- ]"), "_")

    val entries = ArrayList<OPDSSeriesEntry>()

    val tags: List<String>
        get() {
            val uniqueList = HashSet<String>()

            entries.forEach { uniqueList.addAll(it.tags) }

            return uniqueList.toList()
        }
}