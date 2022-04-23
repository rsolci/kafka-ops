/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package io.github.rsolci.kafkaops

import com.github.ajalt.clikt.core.subcommands
import io.github.rsolci.kafkaops.commands.ApplyCommand
import io.github.rsolci.kafkaops.commands.MainCommand
import io.github.rsolci.kafkaops.commands.PlanCommand

fun main(args: Array<String>) = MainCommand()
    .subcommands(PlanCommand(), ApplyCommand())
    .main(args)
