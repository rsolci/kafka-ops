package io.github.rsolci.kafkaops.models.schema

data class Schema(
    val topics: Map<String, TopicDefinition>,
    val settings: SchemaSettings = SchemaSettings()
)
