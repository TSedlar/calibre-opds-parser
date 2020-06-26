# Calibre OPDS Parser

[![Release](https://jitpack.io/v/tsedlar/calibre-opds-parser.svg)](https://jitpack.io/#tsedlar/calibre-opds-parser)
[![](https://img.shields.io/badge/-Donate-orange.svg?logo=Patreon&labelColor=7A7A7A)](https://www.patreon.com/bePatron?c=954360)
[![](https://img.shields.io/badge/-Donate-blue.svg?logo=Paypal&labelColor=7A7A7A)](https://paypal.me/TSedlar)

A library for interacting with Calibre OPDS

## Example

```kotlin
import me.sedlar.calibre.opds.OPDSParser

fun main(args: Array<String>) {
    // args[0] = http://<ip>:<port>
    // args[1] = username
    // args[2] = password
    val parser = OPDSParser(args[0], args[1], args[2])

    val libs = parser.parse()

    libs.forEach { lib ->
        println("Caching images...")
        lib.cacheImages()

        println("Cleaning cache...")
        lib.cleanCache()

        println(lib.name)
        lib.seriesList.forEach { series ->
            println("  ${series.name}")
            series.entries.forEach { entry ->
                println("    ${entry.title}")
                entry.acquisitions.forEach { acquisition ->
                    println("      @ ${lib.getAcquisitionURL(acquisition)}")
                    if (entry.title == "Some Book Title") {
                        lib.downloadAcquisition(series, entry, acquisition)
                    }
                }
            }
        }
    }
}
```