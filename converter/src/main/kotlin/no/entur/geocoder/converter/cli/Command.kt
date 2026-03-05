package no.entur.geocoder.converter.cli

import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.ConverterConfig
import no.entur.geocoder.converter.cli.FileTypeDetector.FileType.*
import no.entur.geocoder.converter.source.adresse.MatrikkelConverter
import no.entur.geocoder.converter.source.osm.OsmConverter
import no.entur.geocoder.converter.source.poi.PoiConverter
import no.entur.geocoder.converter.source.stedsnavn.StedsnavnConverter
import no.entur.geocoder.converter.source.stopplace.StopPlaceConverter
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Command(args).run()
}

class Command(private val args: Array<String>) {
    private val fileTypeDetector = FileTypeDetector()

    fun run() {
        if (args.isEmpty()) {
            printUsage()
            exitProcess(0)
        }

        val action = args[0]
        val rest = args.drop(1).toTypedArray()

        when (action.lowercase()) {
            "-h", "--help" -> { printUsage(); exitProcess(0) }
            "stopplace" -> runStopPlace(rest)
            "matrikkel" -> runMatrikkel(rest)
            "osm" -> runOsm(rest)
            "stedsnavn" -> runStedsnavn(rest)
            "poi" -> runPoi(rest)
            else -> exit("Unknown action '$action'.")
        }
    }

    private fun runStopPlace(args: Array<String>) {
        val opts = parseOptions(args)
        val config = readConfig(opts.configPath)
        val inputFile = validateInput(opts.inputPath, XML, "stopplace")
        runConversion("StopPlace", StopPlaceConverter(config), inputFile, opts)
    }

    private fun runMatrikkel(args: Array<String>) {
        val opts = parseOptions(args, valueFlags = setOf("-g"), booleanFlags = setOf("--no-county"))
        val stedsnavnPath = opts.extra["-g"]
        val noCounty = opts.extra.containsKey("--no-county")

        if (stedsnavnPath == null && !noCounty) {
            exit("matrikkel requires -g <stedsnavn.gml> for county data, or --no-county to skip it.")
        }

        val stedsnavnFile = stedsnavnPath?.let {
            val f = File(it)
            if (!f.exists()) exit("Stedsnavn file does not exist: ${f.absolutePath}")
            fileTypeDetector.validateFileType(f, GML, "-g")
            f
        }

        val config = readConfig(opts.configPath)
        val inputFile = validateInput(opts.inputPath, CSV, "matrikkel")
        runConversion("Matrikkel", MatrikkelConverter(stedsnavnFile, config), inputFile, opts)
    }

    private fun runOsm(args: Array<String>) {
        val opts = parseOptions(args)
        val config = readConfig(opts.configPath)
        val inputFile = validateInput(opts.inputPath, PBF, "osm")
        runConversion("OSM PBF", OsmConverter(config), inputFile, opts)
    }

    private fun runStedsnavn(args: Array<String>) {
        val opts = parseOptions(args)
        val config = readConfig(opts.configPath)
        val inputFile = validateInput(opts.inputPath, GML, "stedsnavn")
        runConversion("Stedsnavn", StedsnavnConverter(config), inputFile, opts)
    }

    private fun runPoi(args: Array<String>) {
        val opts = parseOptions(args)
        val config = readConfig(opts.configPath)
        val inputFile = validateInput(opts.inputPath, XML, "poi")
        runConversion("POI", PoiConverter(config), inputFile, opts)
    }

    private fun runConversion(name: String, converter: Converter, inputFile: File, opts: Options) {
        val outputFile = File(opts.outputPath)

        if (outputFile.exists()) {
            if (!opts.forceOverwrite && !opts.append) {
                exit("Output file '${outputFile.absolutePath}' already exists. Use -f to overwrite or -a to append.")
            }
            if (opts.forceOverwrite) {
                println("Overwriting existing file: ${outputFile.absolutePath}")
                outputFile.delete()
            } else if (opts.append) {
                println("Appending to existing file: ${outputFile.absolutePath}")
            }
        }

        println("Starting $name conversion...")
        val startTime = System.currentTimeMillis()
        converter.convert(inputFile, outputFile, opts.append)
        val durationSeconds = (System.currentTimeMillis() - startTime) / 1000.0
        val fileSizeMB = outputFile.length() / (1024.0 * 1024.0)
        val action = if (opts.append) "Appended to" else "Output written to"
        println("$name conversion completed in %.2f seconds. $action ${outputFile.absolutePath}, size: %.2f MB.".format(durationSeconds, fileSizeMB))
    }

    private data class Options(
        val inputPath: String,
        val outputPath: String,
        val configPath: String?,
        val forceOverwrite: Boolean,
        val append: Boolean,
        val extra: Map<String, String?>,
    )

    private fun parseOptions(
        args: Array<String>,
        valueFlags: Set<String> = emptySet(),
        booleanFlags: Set<String> = emptySet(),
    ): Options {
        var inputPath: String? = null
        var outputPath: String? = null
        var configPath: String? = null
        var forceOverwrite = false
        var append = false
        val extra = mutableMapOf<String, String?>()

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "-i" -> {
                    if (i + 1 >= args.size) exit("-i requires an argument.")
                    inputPath = args[i + 1]; i += 2
                }
                "-o" -> {
                    if (i + 1 >= args.size) exit("-o requires an argument.")
                    outputPath = args[i + 1]; i += 2
                }
                "-c" -> {
                    if (i + 1 >= args.size) exit("-c requires an argument.")
                    configPath = args[i + 1]; i += 2
                }
                "-f" -> { forceOverwrite = true; i += 1 }
                "-a" -> { append = true; i += 1 }
                "-h", "--help" -> { printUsage(); exitProcess(0) }
                in valueFlags -> {
                    val flag = args[i]
                    if (i + 1 >= args.size) exit("$flag requires an argument.")
                    extra[flag] = args[i + 1]; i += 2
                }
                in booleanFlags -> {
                    extra[args[i]] = null; i += 1
                }
                else -> exit("Unknown option: ${args[i]}")
            }
        }

        if (outputPath == null) exit("-o <output-file> is required.")
        if (inputPath == null) exit("-i <input-file> is required.")
        if (forceOverwrite && append) exit("Cannot use both -f and -a together.")

        return Options(inputPath, outputPath, configPath, forceOverwrite, append, extra)
    }

    private fun validateInput(path: String, expectedType: FileTypeDetector.FileType, action: String): File {
        val inputFile = File(path)
        if (!inputFile.exists()) {
            exit("Input file does not exist: ${inputFile.absolutePath}")
        }
        try {
            fileTypeDetector.validateFileType(inputFile, expectedType, action)
        } catch (e: IllegalArgumentException) {
            exit(e.message ?: "File validation failed")
        }
        return inputFile
    }

    internal fun readConfig(configPath: String?): ConverterConfig {
        val configFile = if (configPath != null) File(configPath) else File("converter.json")
        val config = ConverterConfig.load(configFile)
        println("Loaded configuration from: ${configFile.absolutePath}")
        return config
    }

    private fun exit(msg: String): Nothing {
        System.err.println("Error: $msg")
        System.err.println("Run 'convert --help' for usage.")
        exitProcess(1)
    }

    fun printUsage() {
        println(
            """
            Usage: convert <action> -i <input-file> -o <output-file> [options]

            Actions:
              stopplace    Convert StopPlace NeTEx XML
              matrikkel    Convert Matrikkel CSV data
              osm          Convert OSM PBF data
              stedsnavn    Convert Stedsnavn GML data
              poi          Convert POI NeTEx XML data

            Options:
              -i <file>    Input file (required)
              -o <file>    Output file (required)
              -c <file>    Configuration file (defaults to converter.json)
              -a           Append to existing output file
              -f           Force overwrite if output file exists
              -h, --help   Show this help

            Matrikkel-specific options:
              -g <file>    Stedsnavn GML file for county data
              --no-county  Skip county population (when -g is not provided)
            """.trimIndent(),
        )
    }
}
