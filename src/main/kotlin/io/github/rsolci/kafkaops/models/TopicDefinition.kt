package io.github.rsolci.kafkaops.models

data class TopicDefinition(
    val partitions: Int,
    val replication: Int,
    val config: Map<String, String> = emptyMap()
)
