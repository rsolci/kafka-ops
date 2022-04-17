package io.github.rsolci.kafkaops.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class MainCommand : CliktCommand() {
    private val schemaFile by option("-s", "--schema", help = "Schema file to sync changes from")
        .file(canBeDir = false, mustBeReadable = true)
    private val allowDelete by option(
        "-d", "--allow-delete",
        help = "Allow the tool to remove resources not found on state file"
    )
        .flag(default = false)
    private val brokers by option("-b", "--brokers", help = "Cluster broker urls", envvar = "KAFKA_BROKERS")
        .default("localhost:9096")

    override fun run() {
        currentContext.obj = RunParams(
            schemaFile = schemaFile,
            allowDelete = allowDelete,
            brokers = brokers
        )
    }
}

data class RunParams(
    val schemaFile: File?,
    val allowDelete: Boolean = false,
    val brokers: String,
)
