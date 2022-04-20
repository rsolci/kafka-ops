package io.github.rsolci.kafkaops.services

import io.github.rsolci.kafkaops.models.plan.ClusterPlan
import io.github.rsolci.kafkaops.models.plan.PartitionPlan
import io.github.rsolci.kafkaops.models.plan.PlanAction
import io.github.rsolci.kafkaops.models.plan.ReplicationPlan
import io.github.rsolci.kafkaops.models.plan.TopicConfigPlan
import io.github.rsolci.kafkaops.models.plan.TopicPlan
import io.github.rsolci.kafkaops.models.schema.Schema
import io.github.rsolci.kafkaops.models.schema.TopicDefinition
import io.github.rsolci.kafkaops.parsers.SchemaFileParser
import mu.KotlinLogging
import org.apache.kafka.clients.admin.ConfigEntry
import org.apache.kafka.clients.admin.TopicDescription
import java.io.File

private val logger = KotlinLogging.logger { }

class PlanService(
    private val schemaFileParser: SchemaFileParser,
    private val kafkaService: KafkaService,
) {

    fun plan(schemaFile: File?, allowDelete: Boolean = false): ClusterPlan {
        logger.info { "[Plan]: Generating plan..." }
        val schema = schemaFileParser.getSchema(schemaFile)
        val existingTopics = kafkaService.getTopics()

        val existingConfigs = kafkaService.getConfigurationForTopics(existingTopics.keys)

        val topicPlans = schema.topics.map { schemaTopicEntry ->
            val topicName = schemaTopicEntry.key
            val schemaTopicDefinition = schemaTopicEntry.value
            val existingTopic = existingTopics[topicName]
            if (existingTopic == null) {
                createNewTopicPlan(topicName, schemaTopicDefinition)
            } else {
                createExistingTopicPlan(topicName, existingTopic, existingConfigs, schemaTopicDefinition)
            }
        }

        val removePlans = generateRemovalPlans(allowDelete, existingTopics, schema)

        return ClusterPlan(
            topicPlans = topicPlans + removePlans
        )
    }

    private fun createExistingTopicPlan(
        topicName: String,
        existingTopic: TopicDescription,
        existingConfigs: Map<String, List<ConfigEntry>>,
        schemaTopicDefinition: TopicDefinition
    ): TopicPlan {
        // TODO ignore topics on deny list
        logger.info { "[Plan]: $topicName exists. Checking configuration" }
        val partitionPlan = generatePartitionPlan(existingTopic, topicName, schemaTopicDefinition)

        val replicationPlan = generateReplicationPlan(existingTopic, schemaTopicDefinition)

        val topicConfigPlans =
            createTopicConfigPlans(existingConfigs[topicName] ?: emptyList(), schemaTopicDefinition)

        val action = if (partitionPlan.action == PlanAction.UPDATE ||
            replicationPlan.action == PlanAction.UPDATE ||
            topicConfigPlans.any { it.action != PlanAction.DO_NOTHING }
        ) PlanAction.UPDATE else PlanAction.DO_NOTHING

        return TopicPlan(
            name = topicName,
            partitionPlan = partitionPlan,
            replicationPlan = replicationPlan,
            topicConfigPlans = topicConfigPlans,
            action = action
        )
    }

    private fun createNewTopicPlan(
        topicName: String,
        schemaTopicDefinition: TopicDefinition
    ): TopicPlan {
        logger.info { "[Plan]: $topicName does not exist. Adding to creation step" }

        val topicConfigPlans = createTopicConfigPlans(emptyList(), schemaTopicDefinition)

        return TopicPlan(
            name = topicName,
            partitionPlan = PartitionPlan(
                newValue = schemaTopicDefinition.partitions,
                action = PlanAction.ADD
            ),
            replicationPlan = ReplicationPlan(
                newValue = schemaTopicDefinition.replication,
                action = PlanAction.ADD
            ),
            topicConfigPlans = topicConfigPlans,
            action = PlanAction.ADD
        )
    }

    private fun createTopicConfigPlans(
        existingTopicConfigEntries: List<ConfigEntry>,
        schemaTopicDefinition: TopicDefinition
    ): List<TopicConfigPlan> {
        val customTopicConfigs =
            existingTopicConfigEntries.filter { it.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG }

        val updateConfigs = customTopicConfigs.map { customConfig ->
            val existingConfigName = customConfig.name()
            val existingConfigValue = customConfig.value()

            val newConfigValue = schemaTopicDefinition.config[existingConfigName]

            if (newConfigValue == null) {
                TopicConfigPlan(
                    key = existingConfigName,
                    previousValue = existingConfigValue,
                    action = PlanAction.REMOVE
                )
            } else if (existingConfigValue == newConfigValue) {
                TopicConfigPlan(
                    key = existingConfigName,
                    previousValue = existingConfigValue,
                    newValue = newConfigValue,
                    action = PlanAction.DO_NOTHING
                )
            } else {
                TopicConfigPlan(
                    key = existingConfigName,
                    previousValue = existingConfigValue,
                    newValue = newConfigValue,
                    action = PlanAction.UPDATE
                )
            }
        }

        val newConfigKeys = schemaTopicDefinition.config.keys.subtract(customTopicConfigs.map { it.name() }.toSet())
        val newConfigs = newConfigKeys.map { newConfigKey ->
            TopicConfigPlan(
                key = newConfigKey,
                newValue = schemaTopicDefinition.config[newConfigKey],
                action = PlanAction.ADD
            )
        }

        return updateConfigs + newConfigs
    }

    private fun generateRemovalPlans(
        allowDelete: Boolean,
        existingTopics: MutableMap<String, TopicDescription>,
        schema: Schema
    ): List<TopicPlan> {
        val topicsToRemove = if (allowDelete) existingTopics.keys.subtract(schema.topics.keys) else emptySet()
        val removePlans = topicsToRemove.map { topicName ->
            logger.info { "[Plan]: $topicName not found in schema. Planning removal" }
            TopicPlan(
                name = topicName,
                action = PlanAction.REMOVE,
                partitionPlan = PartitionPlan(
                    newValue = 0,
                    action = PlanAction.REMOVE
                ),
                replicationPlan = ReplicationPlan(
                    newValue = 0,
                    action = PlanAction.REMOVE
                ),
            )
        }
        return removePlans
    }

    private fun generateReplicationPlan(
        existingTopic: TopicDescription,
        schemaTopicDefinition: TopicDefinition
    ): ReplicationPlan {
        val desiredReplication = schemaTopicDefinition.replication
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
