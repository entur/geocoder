package no.entur.geocoder.converter.cli

import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.source.matrikkel.MatrikkelConverter
import no.entur.geocoder.converter.source.netex.StopPlaceConverter
import no.entur.geocoder.converter.source.osm.OsmConverter
import no.entur.geocoder.converter.source.stedsnavn.StedsnavnConverter
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Command(args).init()
}

class Command(private val args: Array<String>) {
    private val fileTypeDetector = FileTypeDetector()

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
            exit("Error: No conversion specified. Use -s for stopplace, -m for matrikkel, -p for OSM PBF, and/or -g for Stedsnavn GML.")
        }

        if (matrikkelInputPath != null && stedsnavnInputPath == null && !noCounty) {
            exit("Error: Matrikkel conversion requires either -g <stedsnavn-gml> to populate county data, or --no-county to skip it.")
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
                ConversionTask("StopPlace", stopplaceInputPath, StopPlaceConverter(), FileTypeDetector.FileType.XML, "-s"),
                ConversionTask("Matrikkel", matrikkelInputPath, MatrikkelConverter(stedsnavnFile), FileTypeDetector.FileType.CSV, "-m"),
                ConversionTask("OSM PBF", osmInputPath, OsmConverter(), FileTypeDetector.FileType.PBF, "-p"),
                ConversionTask("Stedsnavn GML", stedsnavnConversionPath, StedsnavnConverter(), FileTypeDetector.FileType.GML, "-g"),
            )

        for (task in conversionTasks) {
            if (task.path != null) {
                val inputFile = readAndValidateFile(task.path, task.expectedType, task.flagName)
                if (!isFirstConversion) {
                    println("\nAppending ${task.name} conversion...")
                } else {
                    println("Starting ${task.name} conversion...")
                }

                val startTime = System.currentTimeMillis()
                task.converter.convert(inputFile, outputFile, !isFirstConversion)
                val endTime = System.currentTimeMillis()
                val durationSeconds = (endTime - startTime) / 1000.0

                val action = if (isFirstConversion) "Output written to" else "Appended to"
                val fileSizeMB = outputFile.length() / (1024.0 * 1024.0)
                println(
                    "${task.name} conversion completed in %.2f seconds. $action ${outputFile.absolutePath}, size: %.2f MB.".format(
                        durationSeconds,
                        fileSizeMB,
                    ),
                )
                isFirstConversion = false
            }
        }
    }

    private data class ConversionTask(
        val name: String,
        val path: String?,
        val converter: Converter,
        val expectedType: FileTypeDetector.FileType,
        val flagName: String,
    )

    private fun readAndValidateFile(path: String, expectedType: FileTypeDetector.FileType, flagName: String): File {
        val inputFile = File(path)
        if (!inputFile.exists()) {
            exit("Error: Input file does not exist: ${inputFile.absolutePath}")
        }

        try {
            fileTypeDetector.validateFileType(inputFile, expectedType, flagName)
        } catch (e: IllegalArgumentException) {
            exit(e.message ?: "Error: File validation failed")
        }

        return inputFile
    }

    private fun exit(msg: String): Nothing {
        println(msg + "\n")
        printUsage()
        exitProcess(1)
    }

    fun printUsage() {
        println(
            """
            Usage: ./convert.sh [options] -o <output-file>
            
            Options:
              -s <input-xml-file>     Convert StopPlace NeTEx data
              -m <input-csv-file>     Convert Matrikkel CSV data
              -p <input-pbf-file>     Convert OSM PBF data
              -g <input-gml-file>     Convert Stedsnavn GML data
              -o <output-file>        Specify the output file (required)
              -f                      Force overwrite if output file exists
              -a                      Append to existing output file
              --no-county             Skip county population for Matrikkel data (when -m is provided and -g is not)
              --no-stedsnavn          Skip Stedsnavn output when converting Matrikkel (when both -m and -g is provided)
            """.trimIndent(),
        )
    }
}
