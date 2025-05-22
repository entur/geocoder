package no.entur.netex_to_json

import java.io.File
import kotlin.system.exitProcess


class App {
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please provide the path to the XML file as an argument.")
        exitProcess(1)
    }
    val xmlFile = File(args[0])
    if (!xmlFile.exists()) {
        println("The file ${xmlFile.absolutePath} does not exist.")
        exitProcess(1)
    }
    NetexParser().parseXmlFile(xmlFile)
}