package io.github.rsolci.kafkaops.models

data class DesiredState(
    val topics: Map<String, TopicDefinition>
)
