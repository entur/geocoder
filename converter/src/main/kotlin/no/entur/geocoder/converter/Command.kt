package no.entur.geocoder.converter

import no.entur.geocoder.converter.matrikkel.MatrikkelConverter
import no.entur.geocoder.converter.netex.StopPlaceConverter
import no.entur.geocoder.converter.osm.OsmConverter
import no.entur.geocoder.converter.stedsnavn.StedsnavnConverter
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
        var stedsnavnInputPath: String? = null
        var outputPath: String? = null
        var forceOverwrite = false
        var appendMode = false
        var noCounty = false
        var noStedsnavn = false

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

                "-g" -> {
                    if (i + 1 >= args.size) {
                        exit("Error: -g flag requires <input-gml-file> argument.")
                    }
                    stedsnavnInputPath = args[i + 1]
                    i += 2
                }

                "-o" -> {
                    if (i + 1 >= args.size) {
                        exit("Error: -o flag requires <output-file> argument.")
                    }
                    outputPath = args[i + 1]
                    i += 2
                }

                "-f" -> {
                    forceOverwrite = true
                    i += 1
                }

                "-a" -> {
                    appendMode = true
                    i += 1
                }

                "--no-county" -> {
                    noCounty = true
                    i += 1
                }
                "--no-stedsnavn" -> {
                    noStedsnavn = true
                    i += 1
                }


                else -> exit("Error: Unknown option ${args[i]}")
            }
        }

        if (outputPath == null) {
            exit("Error: Output file must be specified with -o <output-file>.")
        }

        if (stopplaceInputPath == null && matrikkelInputPath == null && osmInputPath == null && stedsnavnInputPath == null) {
            exit("Error: No conversion type specified. Use -s for stopplace, -m for matrikkel, -p for OSM PBF, and/or -g for Stedsnavn GML.")
        }

        if (matrikkelInputPath != null && stedsnavnInputPath == null && !noCounty) {
            exit("Error: Matrikkel conversion requires either -g <stedsnavn-gml-file> to populate county data, or --no-county to skip county population.")
        }

        if (forceOverwrite && appendMode) {
            exit("Error: Cannot use both -f (force overwrite) and -a (append) flags together.")
        }

        val outputFile = File(outputPath)

        if (outputFile.exists()) {
            if (!forceOverwrite && !appendMode) {
                exit("Error: Output file '${outputFile.absolutePath}' already exists. Use -f to overwrite or -a to append.")
            }
            if (forceOverwrite) {
                println("Overwriting existing file: ${outputFile.absolutePath}")
                outputFile.delete()
            }
            if (appendMode) {
                println("Appending to existing file: ${outputFile.absolutePath}")
            }
        }

        var isFirstConversion = !appendMode

        val stedsnavnFile = stedsnavnInputPath?.let { File(it) }

        // If --no-stedsnavn is specified, use the Stedsnavn file for mapping only, not for conversion
        val stedsnavnConversionPath = if (noStedsnavn) null else stedsnavnInputPath

        val conversionTasks =
            listOf(
                "StopPlace" to (stopplaceInputPath to StopPlaceConverter()),
                "Matrikkel" to (matrikkelInputPath to MatrikkelConverter(stedsnavnFile)),
                "OSM PBF" to (osmInputPath to OsmConverter()),
                "Stedsnavn GML" to (stedsnavnConversionPath to StedsnavnConverter()),
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
        println(msg + "\n")
        printUsage()
        exitProcess(1)
    }

    fun printUsage() {
        println("Usage: geocoder-convert [options] -o <output-file>")
        println("  --no-stedsnavn          : Skip Stedsnavn import (use -g only for county mapping in Matrikkel).")
        println("Options:")
        println("  -s <input-xml-file>     : Convert StopPlace XML data.")
        println("  -m <input-csv-file>     : Convert Matrikkel CSV data.")
        println("  -p <input-pbf-file>     : Convert OSM PBF data.")
        println("  -g <input-gml-file>     : Convert Stedsnavn GML data.")
        println("  -o <output-file>        : Specify the output file (required).")
        println("  -f                      : Force overwrite if output file exists.")
        println("          geocoder-convert -m matrikkel.csv -g stedsnavn.gml --no-stedsnavn -o output.ndjson")
        println("  -a                      : Append to existing output file (skips header).")
        println("  --no-county             : Skip county population for Matrikkel data (only if -g not provided).")
        println("All conversion options can be used together, outputting to the same -o file.")
        println("Note: When using -m (Matrikkel), you must also provide -g (Stedsnavn GML) to populate county data,")
        println("      or use --no-county to skip county population.")
        println("Examples: geocoder-convert -s stoplace.xml -m matrikkel.csv -p data.osm.pbf -o combined_output.ndjson")
        println("          geocoder-convert -s stoplace.xml -o s_out.ndjson")
        println("          geocoder-convert -s stoplace.xml -o existing.ndjson -f")
        println("          geocoder-convert -m matrikkel.csv -g stedsnavn.gml -o output.ndjson")
        println("          geocoder-convert -m matrikkel.csv --no-county -o output.ndjson")
        println("          geocoder-convert -g stedsnavn.gml -o stedsnavn_output.ndjson")
    }
}
