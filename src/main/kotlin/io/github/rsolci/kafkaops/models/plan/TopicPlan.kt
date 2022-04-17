package io.github.rsolci.kafkaops.models.plan

data class TopicPlan (
    val name: String,
    val partitionPlan: PartitionPlan,
    val replicationPlan: ReplicationPlan,
    val action: PlanAction,
)
