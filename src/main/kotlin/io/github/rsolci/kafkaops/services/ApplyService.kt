package io.github.rsolci.kafkaops.services

import io.github.rsolci.kafkaops.models.plan.ClusterPlan
import io.github.rsolci.kafkaops.models.plan.PlanAction
import io.github.rsolci.kafkaops.models.plan.TopicPlan
import io.github.rsolci.kafkaops.printer.postTopicApplyLog
import io.github.rsolci.kafkaops.printer.preTopicApplyLog

class ApplyService(
    private val kafkaService: KafkaService,
) {

    fun apply(clusterPlan: ClusterPlan) {
        clusterPlan.topicPlans.forEach { topicPlan ->
            if (topicPlan.action == PlanAction.ADD) {
                applyAdd(topicPlan)
            }
        }
    }

    private fun applyAdd(topicPlan: TopicPlan) {
        preTopicApplyLog(topicPlan)
        kafkaService.createTopic(
            name = topicPlan.name,
            partitionCount = topicPlan.partitionPlan.newValue,
            replicationFactor = topicPlan.replicationPlan.newValue.toShort(),
            configs = topicPlan.topicConfigPlans
                .filter { !it.newValue.isNullOrBlank() }
                .associate { it.key to it.newValue!! }
        )
        postTopicApplyLog(topicPlan)
    }
}
