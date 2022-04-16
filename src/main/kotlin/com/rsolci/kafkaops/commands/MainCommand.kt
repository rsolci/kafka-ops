package com.rsolci.kafkaops.commands

import com.github.ajalt.clikt.core.CliktCommand

class MainCommand : CliktCommand() {
    override fun run() {
        echo("Test")
    }
}