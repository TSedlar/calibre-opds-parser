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
    val thumbnail: String,
    val extras: Map<String, String>
): Serializable {

    val uuid = id.replace("urn:uuid:", "")

    val tags: List<String>
        get() {
            val list = ArrayList<String>()

            if (extras.containsKey("tags")) {
                val tags = extras["tags"]
                tags?.split(",")?.forEach {
                    list.add(it.trim())
                }
            }

            return list
        }
}