package io.github.rsolci.kafkaops.models.plan

data class TopicConfigPlan(
    val key: String,
    val newValue: String? = null,
    val previousValue: String? = null,
    val action: PlanAction,
)
