package io.github.rsolci.kafkaops.models.plan

data class PartitionPlan(
    val previousValue: Int? = null,
    val newValue: Int,
    val action: PlanAction,
)
