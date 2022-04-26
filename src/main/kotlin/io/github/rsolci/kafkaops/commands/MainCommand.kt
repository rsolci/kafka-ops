package io.github.rsolci.kafkaops.commands

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import org.slf4j.Logger
import java.io.File

class MainCommand : CliktCommand() {
    private val schemaFile by option("-s", "--schema", help = "Schema file to sync changes from")
        .file(canBeDir = false, mustBeReadable = true)
    private val allowDelete by option(
        "-d", "--allow-delete",
        help = "Allow the tool to remove resources not found on state file"
    )
        .flag(default = false)
    private val verbose by option(
        "-v", "--verbose",
        help = "Show application and kafka library logs"
    ).flag(default = false)

    override fun run() {
        val rootLogger = KotlinLogging.logger(Logger.ROOT_LOGGER_NAME).underlyingLogger as ch.qos.logback.classic.Logger
        val logLevel = if (verbose) Level.INFO else Level.WARN
        rootLogger.level = logLevel

        currentContext.obj = RunParams(
            schemaFile = schemaFile,
            allowDelete = allowDelete,
        )
    }
}

data class RunParams(
    val schemaFile: File?,
    val allowDelete: Boolean = false,
)
