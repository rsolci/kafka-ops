package io.github.rsolci.kafkaops.models

data class Schema(
    val topics: Map<String, TopicDefinition>
)
