package io.github.rsolci.kafkaops.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import io.github.rsolci.kafkaops.config.createKafkaAdminClient
import io.github.rsolci.kafkaops.config.createObjectMapper
import io.github.rsolci.kafkaops.parsers.SchemaFileParser
import io.github.rsolci.kafkaops.printer.printPlan
import io.github.rsolci.kafkaops.services.KafkaService
import io.github.rsolci.kafkaops.services.PlanService

class PlanCommand : CliktCommand(
    name = "plan",
    help = "Plan the execution based on the provided schema file"
) {
    private val config by requireObject<RunParams>()

    override fun run() {
        val adminClient = createKafkaAdminClient()
        val planService = PlanService(
            schemaFileParser = SchemaFileParser(createObjectMapper()),
            kafkaService = KafkaService(adminClient)
        )

        val clusterPlan = planService.plan(schemaFile = config.schemaFile, allowDelete = config.allowDelete)
        printPlan(clusterPlan)
        adminClient.close()
    }
}
