package io.github.rsolci.kafkaops

import io.github.rsolci.kafkaops.models.TopicDefinition
import io.github.rsolci.kafkaops.models.plan.*
import io.github.rsolci.kafkaops.parsers.SchemaFileParser
import io.github.rsolci.kafkaops.services.KafkaService
import mu.KotlinLogging
import org.apache.kafka.clients.admin.TopicDescription
import java.io.File

private val logger = KotlinLogging.logger { }

class PlanService(
    private val schemaFileParser: SchemaFileParser,
    private val kafkaService: KafkaService,
) {

    fun plan(schemaFile: File?, allowDelete: Boolean): ClusterPlan {
        logger.info { "[Plan]: Generating plan..." }
        val schema = schemaFileParser.getSchema(schemaFile)
        val existingTopics = kafkaService.getTopics()

        val topicPlans = schema.topics.map { topicEntry ->
            val topicName = topicEntry.key
            val existingTopic = existingTopics[topicName]
            if (existingTopic == null) {
                logger.info { "[Plan]: $topicName does not exist. Adding to creation step" }
                TopicPlan(
                    name = topicName,
                    partitionPlan = PartitionPlan(
                        newValue = topicEntry.value.partitions,
                        action = PlanAction.ADD
                    ),
                    replicationPlan = ReplicationPlan(
                        newValue = topicEntry.value.replication,
                        action = PlanAction.ADD
                    ),
                    action = PlanAction.ADD
                )
            } else {
                logger.info { "[Plan]: $topicName exists. Checking configuration" }
                val partitionPlan = generatePartitionPlan(existingTopic, topicName, topicEntry.value)

                val replicationPlan = generateReplicationPlan(topicEntry, existingTopic)

                // TODO config

                TopicPlan(
                    name = topicName,
                    partitionPlan = partitionPlan,
                    replicationPlan = replicationPlan,
                    action = if (partitionPlan.action == PlanAction.UPDATE ||
                        replicationPlan.action == PlanAction.UPDATE) PlanAction.UPDATE else PlanAction.DO_NOTHING
                )
            }
        }

        return ClusterPlan(
            topicPlans = topicPlans
        )
    }

    private fun generateReplicationPlan(
        topicEntry: Map.Entry<String, TopicDefinition>,
        existingTopic: TopicDescription
    ): ReplicationPlan {
        val desiredReplication = topicEntry.value.replication
        val existingReplication = existingTopic.partitions()[0].replicas().size
        return ReplicationPlan(
            previousValue = existingReplication,
            newValue = desiredReplication,
            action = if (desiredReplication != existingReplication) PlanAction.UPDATE else PlanAction.DO_NOTHING
        )
    }

    private fun generatePartitionPlan(
        existingTopic: TopicDescription,
        topicName: String,
        desiredTopicDefinition: TopicDefinition
    ): PartitionPlan {
        val desiredPartitions = desiredTopicDefinition.partitions
        val existingPartitions = existingTopic.partitions().size
        return if (desiredPartitions < existingPartitions) {
            logger.error {
                "Topic $topicName cant change partitions from $existingPartitions to $desiredPartitions"
            }
            throw IllegalArgumentException(
                "Removing partitions is not yet supported. " +
                        "Topic: $topicName have $existingPartitions cant change to $desiredPartitions"
            )
        } else if (desiredPartitions > existingPartitions) {
            PartitionPlan(
                previousValue = existingPartitions,
                newValue = desiredPartitions,
                action = PlanAction.UPDATE
            )
        } else {
            PartitionPlan(
                previousValue = existingPartitions,
                newValue = desiredPartitions,
                action = PlanAction.DO_NOTHING
            )
        }
    }
}
