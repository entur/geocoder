package no.entur.netex_photon

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: netex-photon <input-xml-file> <output-file>")
        exitProcess(1)
    }
    val input = File(args[0])
    if (!input.exists()) {
        println("The file ${input.absolutePath} does not exist.")
        exitProcess(1)
    }
    val output = File(args[1])
    Converter().convert(input, output)
}