package no.entur.geocoder.converter

import no.entur.geocoder.converter.matrikkel.MatrikkelConverter
import no.entur.geocoder.converter.netex.StopPlaceConverter
import no.entur.geocoder.converter.osm.OsmConverter
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Command(args).init()
}

class Command(
    private val args: Array<String>,
) {
    fun init() {
        if (args.isEmpty()) {
            exit("Error: No arguments provided.")
        }

        var stopplaceInputPath: String? = null
        var matrikkelInputPath: String? = null
        var osmInputPath: String? = null
        var outputPath: String? = null

        var i = 0
        while (i < args.size) {
            when (args[i].lowercase()) {
                "-s" -> {
                    if (i + 1 >= args.size) {
                        exit("Error: -s flag requires <input-xml-file> argument.")
                    }
                    stopplaceInputPath = args[i + 1]
                    i += 2
                }

                "-m" -> {
                    if (i + 1 >= args.size) {
                        exit("Error: -m flag requires <input-csv-file> argument.")
                    }
                    matrikkelInputPath = args[i + 1]
                    i += 2
                }

                "-p" -> {
                    if (i + 1 >= args.size) {
                        exit("Error: -p flag requires <input-pbf-file> argument.")
                    }
                    osmInputPath = args[i + 1]
                    i += 2
                }

                "-o" -> {
                    if (i + 1 >= args.size) {
                        exit("Error: -o flag requires <output-file> argument.")
                    }
                    outputPath = args[i + 1]
                    i += 2
                }

                else -> exit("Error: Unknown option ${args[i]}")
            }
        }

        if (outputPath == null) {
            exit("Error: Output file must be specified with -o <output-file>.")
        }

        if (stopplaceInputPath == null && matrikkelInputPath == null && osmInputPath == null) {
            exit("Error: No conversion type specified. Use -s for stopplace, -m for matrikkel, and/or -p for OSM PBF.")
        }

        val outputFile = File(outputPath)
        var isFirstConversion = true

        val conversionTasks =
            listOf(
                "StopPlace" to (stopplaceInputPath to StopPlaceConverter()),
                "Matrikkel" to (matrikkelInputPath to MatrikkelConverter()),
                "OSM PBF" to (osmInputPath to OsmConverter()),
            )

        for ((name, pair) in conversionTasks) {
            val (path, converter) = pair
            if (path != null) {
                val inputFile = readFile(path)
                if (!isFirstConversion) {
                    println("\nAppending $name conversion...")
                } else {
                    println("Starting $name conversion...")
                }

                val startTime = System.currentTimeMillis()
                converter.convert(inputFile, outputFile, !isFirstConversion)
                val endTime = System.currentTimeMillis()
                val durationSeconds = (endTime - startTime) / 1000.0

                val action = if (isFirstConversion) "Output written to" else "Appended to"
                val fileSizeMB = outputFile.length() / (1024.0 * 1024.0)
                println(
                    "$name conversion completed in %.2f seconds. $action ${outputFile.absolutePath}, size: %.2f MB.".format(
                        durationSeconds,
                        fileSizeMB
                    )
                )
                isFirstConversion = false
            }
        }
    }

    private fun readFile(path: String): File {
        val inputFile = File(path)
        if (!inputFile.exists()) {
            println("The StopPlace input file ${inputFile.absolutePath} does not exist.")
            exitProcess(1)
        }
        return inputFile
    }

    private fun exit(msg: String): Nothing {
        println(msg)
        printUsage()
        exitProcess(1)
    }

    fun printUsage() {
        println("Usage: geocoder-convert [options] -o <output-file>")
        println("Options:")
        println("  -s <input-xml-file>     : Convert StopPlace XML data.")
        println("  -m <input-csv-file>     : Convert Matrikkel CSV data.")
        println("  -p <input-pbf-file>     : Convert OSM PBF data.")
        println("  -o <output-file>        : Specify the output file (required).")
        println("All conversion options can be used together, outputting to the same -o file.")
        println("Examples: geocoder-convert -s stoplace.xml -m matrikkel.csv -p data.osm.pbf -o combined_output.ndjson")
        println("          geocoder-convert -s stoplace.xml -o s_out.ndjson")
    }
}
