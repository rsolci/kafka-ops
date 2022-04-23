package io.github.rsolci.kafkaops.services

import io.github.rsolci.kafkaops.models.plan.ClusterPlan
import io.github.rsolci.kafkaops.models.plan.PlanAction
import io.github.rsolci.kafkaops.models.plan.TopicPlan
import io.github.rsolci.kafkaops.printer.postTopicApplyLog
import io.github.rsolci.kafkaops.printer.preTopicApplyLog

class ApplyService(
    private val kafkaService: KafkaService,
) {

    fun apply(clusterPlan: ClusterPlan, allowDelete: Boolean = false) {
        clusterPlan.topicPlans.forEach { topicPlan ->
            preTopicApplyLog(topicPlan)
            if (topicPlan.action == PlanAction.ADD) {
                applyAdd(topicPlan)
            } else if (topicPlan.action == PlanAction.UPDATE) {
                applyUpdate(topicPlan)
            } else if (allowDelete && topicPlan.action == PlanAction.REMOVE) {
                kafkaService.deleteTopic(topicPlan.name)
            }
            postTopicApplyLog(topicPlan)
        }
    }

    private fun applyUpdate(topicPlan: TopicPlan) {
        if (topicPlan.partitionPlan.action == PlanAction.UPDATE) {
            kafkaService.increaseTopicPartitions(topicPlan.name, topicPlan.partitionPlan.newValue)
        }
        if (topicPlan.replicationPlan.action == PlanAction.UPDATE) {
            kafkaService.updateTopicReplication(topicPlan.name, topicPlan.replicationPlan.newValue)
        }

        if (topicPlan.topicConfigPlans.any { it.action != PlanAction.DO_NOTHING }) {
            kafkaService.updateTopicConfig(topicPlan.name, topicPlan.topicConfigPlans)
        }
    }

    private fun applyAdd(topicPlan: TopicPlan) {
        kafkaService.createTopic(
            name = topicPlan.name,
            partitionCount = topicPlan.partitionPlan.newValue,
            replicationFactor = topicPlan.replicationPlan.newValue.toShort(),
            configs = topicPlan.topicConfigPlans
                .filter { !it.newValue.isNullOrBlank() }
                .associate { it.key to it.newValue!! }
        )
    }
}
