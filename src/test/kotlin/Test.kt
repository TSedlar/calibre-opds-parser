import me.sedlar.calibre.opds.OPDSParser

fun main(args: Array<String>) {
    val parser = OPDSParser(args[0], args[1], args[2])

    val libs = parser.parse()

    libs.forEach { lib ->
        println(lib.name)
        lib.seriesList.forEach { series ->
            println("  ${series.name}")
            series.entries.forEach { entry ->
                println("    ${entry.title}")
                entry.acquisitions.forEach { acq ->
                    println("      ${acq.type} -> ${acq.fileExtension}")
                }
            }
        }
//        lib.cacheImages()
//        lib.cleanCache()
    }
}