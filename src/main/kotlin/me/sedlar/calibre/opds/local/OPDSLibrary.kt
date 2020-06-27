package me.sedlar.calibre.opds.local

import me.sedlar.calibre.opds.OPDSConnector
import me.sedlar.calibre.opds.model.OPDSAcquisition
import me.sedlar.calibre.opds.model.OPDSEntry
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import java.io.File
import java.nio.file.Files

class OPDSLibrary(
    private val baseURL: String,
    private val username: String,
    private val password: String,
    private val dataDir: File,
    entry: OPDSEntry
) {

    val id = entry.id
    val name = entry.id.replace("calibre-library:", "")

    val seriesList = ArrayList<OPDSSeries>()

    /**
     * Gets the full URL to the given acquisition
     *
     * @param acquisition The acquisition to get a URL for
     *
     * @return The full URL of the given acquisition
     */
    fun getAcquisitionURL(acquisition: OPDSAcquisition): String {
        return baseURL + acquisition.link
    }

    /**
     * Gets the full URL to the given cover link
     *
     * @param coverLink The cover to get a URL for
     *
     * @return The full URL of the given cover
     */
    fun getCoverURL(coverLink: String): String {
        return baseURL + coverLink
    }

    /**
     * Gets the full URL to the given thumbnail link
     *
     * @param thumbLink The thumbnail to get a URL for
     *
     * @return The full URL of the given thumbnail
     */
    fun getThumbURL(thumbLink: String): String {
        return baseURL + thumbLink
    }

    /**
     * Gets the File in which the series entry cover would be written to
     *
     * @param series The series that the cover is a part of
     * @param seriesEntry The entry in which the cover is attached to
     *
     * @return The File in which the series entry cover would be written to
     */
    fun getCoverFile(series: OPDSSeries, seriesEntry: OPDSSeriesEntry): File {
        return File(dataDir, "libs/$name/covers/${series.pathName}/${seriesEntry.uuid}.jpg")
    }

    /**
     * Gets the File in which the series entry thumbnail would be written to
     *
     * @param series The series that the thumbnail is a part of
     * @param seriesEntry The entry in which the thumbnail is attached to
     *
     * @return The File in which the series entry thumbnail would be written to
     */
    fun getThumbFile(series: OPDSSeries, seriesEntry: OPDSSeriesEntry): File {
        return File(dataDir, "libs/$name/thumbs/${series.pathName}/${seriesEntry.uuid}.jpg")
    }

    /**
     * Downloads the given acquisition to the local disk
     *
     * @param series The series that the cover is a part of
     * @param seriesEntry The entry in which the cover is attached to
     * @param acquisition The acquisition to download
     *
     * @return Whether or not the acquisition was downloaded without error
     */
    fun downloadAcquisition(series: OPDSSeries, seriesEntry: OPDSSeriesEntry, acquisition: OPDSAcquisition): Boolean {
        val acquisitionBytes = OPDSConnector.readBytesByDigest(getAcquisitionURL(acquisition), username, password)

        val targetExt = when (acquisition.type) {
            "application/epub+zip" -> "epub"
            else -> "zip"
        }

        val targetFile = File(dataDir, "libs/$name/downloads/${series.pathName}/${seriesEntry.uuid}.${targetExt}")

        return if (acquisitionBytes != null) {
            targetFile.parentFile.mkdirs()
            Files.write(targetFile.toPath(), acquisitionBytes)
            true
        } else {
            false
        }
    }

    /**
     * Caches all covers and thumbnails for every series in the library
     *
     * @param preSeriesCallback A callback called prior to running on a series
     * @param postSeriesCallback A callback called after running on a series
     * @param preEntryCallback A callback ran before running on an entry in a series
     * @param postEntryCallback A callback ran after running on an entry in a series
     */
    fun cacheImages(
        preSeriesCallback: ((series: OPDSSeries) -> Unit)? = null,
        postSeriesCallback: ((series: OPDSSeries) -> Unit)? = null,
        preEntryCallback: ((series: OPDSSeries, entry: OPDSSeriesEntry) -> Unit)? = null,
        postEntryCallback: ((series: OPDSSeries, entry: OPDSSeriesEntry) -> Unit)? = null
    ) {
        seriesList.forEach { series ->
            preSeriesCallback?.let { it(series) }
            series.entries.forEach { entry ->
                preEntryCallback?.let { it(series, entry) }

                val targetCoverFile = getCoverFile(series, entry)
                val targetThumbFile = getThumbFile(series, entry)

                if (!targetCoverFile.exists()) {
                    OPDSConnector.readBytesByDigest(getCoverURL(entry.cover), username, password)?.let { coverBytes ->
                        targetCoverFile.parentFile.mkdirs()
                        Files.write(targetCoverFile.toPath(), coverBytes)
                    }
                }

                if (!targetThumbFile.exists()) {
                    OPDSConnector.readBytesByDigest(getThumbURL(entry.cover), username, password)?.let { coverBytes ->
                        targetThumbFile.parentFile.mkdirs()
                        Files.write(targetThumbFile.toPath(), coverBytes)
                    }
                }

                postEntryCallback?.let { it(series, entry) }
            }
            postSeriesCallback?.let { it(series) }
        }
    }

    /**
     * Removes series data that no longer exists
     */
    fun cleanCache() {
        // Delete series data that does not exist any more
        val seriesListFolder = File(dataDir, "libs/$name/series-list/")
        seriesListFolder.listFiles()?.forEach { seriesFolder ->
            val seriesName = seriesFolder.name
            val hasXML = seriesFolder.listFiles()?.any { it.toPath().endsWith(".xml") } ?: false
            val doesSeriesExist = seriesList.any { it.pathName == seriesName }
            if (hasXML && doesSeriesExist) {
                val coverFolder = File(dataDir, "covers/$seriesName/")
                val thumbFolder = File(dataDir, "thumbs/$seriesName/")
                seriesFolder.deleteRecursively()
                coverFolder.deleteRecursively()
                thumbFolder.deleteRecursively()
            }
        }

        // Delete covers/thumbnails that do not exist any more
        seriesList.forEach { series ->
            val coverFolder = File(dataDir, "libs/$name/covers/${series.pathName}/")
            val thumbFolder = File(dataDir, "libs/$name/thumbs/${series.pathName}/")

            val validUUIDList = ArrayList<String>()
            series.entries.forEach { entry ->
                validUUIDList.add(entry.uuid)
            }

            coverFolder.listFiles()?.forEach { cover ->
                val coverFileName = cover.toPath().fileName.toString()
                // Check for jpg image and name containing 4 dashes
                if (coverFileName.endsWith(".jpg") && coverFileName.split("-").size == 5) {
                    val doesUUIDExist = validUUIDList.any { "$it.jpg" == coverFileName }
                    if (!doesUUIDExist) {
                        println("Removed non-existing series entry cover: $coverFileName")
                        cover.delete()
                    }
                }
            }

            thumbFolder.listFiles()?.forEach { thumb ->
                val thumbFileName = thumb.toPath().fileName.toString()
                // Check for jpg image and name containing 4 dashes
                if (thumbFileName.endsWith(".jpg") && thumbFileName.split("-").size == 5) {
                    val doesUUIDExist = validUUIDList.any { "$it.jpg" == thumbFileName }
                    if (!doesUUIDExist) {
                        println("Removed non-existing series entry thumb: $thumbFileName")
                        thumb.delete()
                    }
                }
            }
        }
    }
}