package io.github.rsolci.kafkaops.models.plan

data class ReplicationPlan(
    val previousValue: Int? = null,
    val newValue: Int,
    val action: PlanAction,
)
