package io.github.rsolci.kafkaops.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class MainCommand : CliktCommand() {
    private val stateFile by option("-s", "--schema", help = "State file to sync changes from")
        .file(canBeDir = false, mustBeReadable = true)
    private val allowDelete by option(
        "-d", "--allow-delete",
        help = "Allow the tool to remove resources not found on state file"
    )
        .flag(default = false)

    override fun run() {
        echo("Test $allowDelete")
        currentContext.obj = RunParams(
            stateFile = requireNotNull(stateFile)
        )
    }
}

data class RunParams(
    val stateFile: File
)
