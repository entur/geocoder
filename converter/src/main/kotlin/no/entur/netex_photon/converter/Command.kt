package no.entur.netex_photon.converter

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Command(args).init()
}

class Command(private val args: Array<String>) {
    fun init() {
        if (args.isEmpty()) {
            exit("Error: No arguments provided.")
        }

        var stopplaceInputPath: String? = null
        var matrikkelInputPath: String? = null
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

        if (stopplaceInputPath == null && matrikkelInputPath == null) {
            exit("Error: No conversion type specified. Use -s for stopplace and/or -m for matrikkel.")
        }

        val outputFile = File(outputPath)
        var isFirstConversion = true

        if (stopplaceInputPath != null) {
            val inputFile = readFile(stopplaceInputPath)
            println("Starting StopPlace conversion...")

            StopPlaceConverter().convert(inputFile, outputFile, !isFirstConversion)
            println("StopPlace conversion completed. Output written to ${outputFile.absolutePath}, size: ${outputFile.length()} bytes.")
            isFirstConversion = false
        }

        if (matrikkelInputPath != null) {
            val inputFile = readFile(matrikkelInputPath)
            if (!isFirstConversion) println("\nAppending Matrikkel conversion...")
            else println("Starting Matrikkel conversion...")

            MatrikkelConverter().convertCsv(inputFile, outputFile, !isFirstConversion)
            println("Matrikkel conversion completed. Appended to ${outputFile.absolutePath}, new size: ${outputFile.length()} bytes.")
        }
    }

    private fun readFile(stopplaceInputPath: String): File {
        val inputFile = File(stopplaceInputPath)
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
        println("Usage: netex-photon [options] -o <output-file>")
        println("Options:")
        println("  -s <input-xml-file>     : Convert StopPlace XML data.")
        println("  -m <input-csv-file>     : Convert Matrikkel CSV data.")
        println("  -o <output-file>        : Specify the output file (required).")
        println("Both -s and -m can be used together, outputting to the same -o file.")
        println("Example: netex-photon -s stoplace.xml -m matrikkel.csv -o combined_output.ndjson")
        println("Example (single): netex-photon -s stoplace.xml -o s_out.ndjson")
    }
}
