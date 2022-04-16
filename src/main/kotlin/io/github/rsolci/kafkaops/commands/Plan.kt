package io.github.rsolci.kafkaops.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject

class Plan : CliktCommand() {
    private val config by requireObject<RunParams>()

    override fun run() {
        echo("Plan? ${config.stateFile}")
    }
}
