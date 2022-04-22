package io.github.rsolci.kafkaops.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import io.github.rsolci.kafkaops.config.createKafkaAdminClient
import io.github.rsolci.kafkaops.config.createObjectMapper
import io.github.rsolci.kafkaops.parsers.SchemaFileParser
import io.github.rsolci.kafkaops.printer.printPlan
import io.github.rsolci.kafkaops.services.KafkaService
import io.github.rsolci.kafkaops.services.PlanService

class PlanCommand : CliktCommand() {
    private val config by requireObject<RunParams>()

    private val planService = PlanService(
        schemaFileParser = SchemaFileParser(createObjectMapper()),
        kafkaService = KafkaService(createKafkaAdminClient())
    )

    override fun run() {
        val clusterPlan = planService.plan(schemaFile = config.schemaFile, allowDelete = config.allowDelete)
        printPlan(clusterPlan)
    }
}
