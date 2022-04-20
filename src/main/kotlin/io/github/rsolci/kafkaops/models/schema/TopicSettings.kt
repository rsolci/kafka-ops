package io.github.rsolci.kafkaops.models.schema

data class TopicSettings(
    val ignoreList: List<String> = emptyList()
)
