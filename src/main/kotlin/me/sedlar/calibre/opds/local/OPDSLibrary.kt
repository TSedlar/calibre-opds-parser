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

    fun getAcquisitionURL(acquisition: OPDSAcquisition): String {
        return baseURL + acquisition.link
    }

    fun getCoverURL(coverLink: String): String {
        return baseURL + coverLink
    }

    fun getThumbURL(thumbLink: String): String {
        return baseURL + thumbLink
    }

    fun getCoverFile(series: OPDSSeries, seriesEntry: OPDSSeriesEntry): File {
        return File(dataDir, "libs/$name/covers/${series.name}/${seriesEntry.uuid}.jpg")
    }

    fun getThumbFile(series: OPDSSeries, seriesEntry: OPDSSeriesEntry): File {
        return File(dataDir, "libs/$name/thumbs/${series.name}/${seriesEntry.uuid}.jpg")
    }

    fun downloadAcquisition(series: OPDSSeries, entry: OPDSSeriesEntry, acquisition: OPDSAcquisition): Boolean {
        val acquisitionBytes = OPDSConnector.readBytesByDigest(getAcquisitionURL(acquisition), username, password)

        val targetExt = when (acquisition.type) {
            "application/epub+zip" -> "epub"
            else -> "zip"
        }

        val targetFile = File(dataDir, "libs/$name/downloads/${series.name}/${entry.uuid}.${targetExt}")

        return if (acquisitionBytes != null) {
            targetFile.parentFile.mkdirs()
            Files.write(targetFile.toPath(), acquisitionBytes)
            true
        } else {
            false
        }
    }

    fun cacheImages() {
        seriesList.forEach { series ->
            series.entries.forEach { entry ->
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
            }
        }
    }

    fun cleanCache() {
        // Delete series data that does not exist any more
        val seriesListFolder = File(dataDir, "libs/$name/series-list/")
        seriesListFolder.listFiles()?.forEach { file ->
            val path = file.toPath()
            if (path.fileName.toString().endsWith(".xml")) {
                val seriesName = path.fileName.toString().dropLast(4)
                val coverFolder = File(dataDir, "covers/$seriesName/")
                val thumbFolder = File(dataDir, "thumbs/$seriesName/")
                val doesSeriesExist = seriesList.any { it.name == seriesName }
                if (!doesSeriesExist) { // Remove all series data
                    println("Removing non-existing series: $seriesName")
                    file.delete() // delete series-list/{series}.xml
                    coverFolder.deleteRecursively()
                    thumbFolder.deleteRecursively()
                }
            }
        }

        // Delete covers/thumbnails that do not exist any more
        seriesList.forEach { series ->
            val coverFolder = File(dataDir, "libs/$name/covers/${series.name}/")
            val thumbFolder = File(dataDir, "libs/$name/thumbs/${series.name}/")

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