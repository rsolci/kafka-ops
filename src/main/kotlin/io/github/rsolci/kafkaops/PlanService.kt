package io.github.rsolci.kafkaops

import io.github.rsolci.kafkaops.parsers.SchemaFileParser
import java.io.File

class PlanService(
    private val schemaFileParser: SchemaFileParser
) {

    fun plan(schemaFile: File?) {
        val schema = schemaFileParser.getSchema(schemaFile)
    }
}
