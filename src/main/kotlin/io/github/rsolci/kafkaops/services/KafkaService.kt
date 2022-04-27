package io.github.rsolci.kafkaops.services

import com.github.ajalt.clikt.core.PrintMessage
import io.github.rsolci.kafkaops.models.plan.PlanAction
import io.github.rsolci.kafkaops.models.plan.TopicConfigPlan
import mu.KotlinLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AlterConfigOp
import org.apache.kafka.clients.admin.ConfigEntry
import org.apache.kafka.clients.admin.NewPartitionReassignment
import org.apache.kafka.clients.admin.NewPartitions
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.common.Node
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.ConfigResource
import java.util.Optional
import java.util.concurrent.ExecutionException

private val logger = KotlinLogging.logger { }

class KafkaService(
    private val adminClient: AdminClient
) {

    fun getTopics(): MutableMap<String, TopicDescription> {
        logger.info { "Listing topics from cluster" }
        val allTopics = adminClient.listTopics().names().get()
        return adminClient.describeTopics(allTopics).allTopicNames().get()
    }

    fun getConfigurationForTopics(topicNames: Set<String>): Map<String, List<ConfigEntry>> {
        val configResources = topicNames.map { ConfigResource(ConfigResource.Type.TOPIC, it) }
        val configResult = try {
            adminClient.describeConfigs(configResources).all().get()
        } catch (e: InterruptedException) {
            logger.error(e) { "Error while fetching topic configuration" }
            throw IllegalStateException("Could not load topic configuration", e)
        } catch (e: ExecutionException) {
            logger.error(e) { "Error while fetching topic configuration" }
            throw IllegalStateException("Could not load topic configuration", e)
        }

        return configResult.entries.associate { it.key.name() to it.value.entries().toList() }
    }

    fun createTopic(name: String, partitionCount: Int, replicationFactor: Short, configs: Map<String, String>) {
        logger.info {
            "Creating topic: $name with $partitionCount partitions and replication factor $replicationFactor"
        }
        val newTopic = NewTopic(name, partitionCount, replicationFactor)
        newTopic.configs(configs)
        try {
            adminClient.createTopics(listOf(newTopic)).all().get()
        } catch (expected: Exception) {
            logger.error(expected) { "Error while creating topic: $name" }
            throw PrintMessage("Could not create topic: $name.\n${expected.message}", error = true)
        }
    }

    fun increaseTopicPartitions(topicName: String, partitions: Int) {
        logger.info { "Increasing topic: $topicName partitions to: $partitions" }
        val newPartitions = NewPartitions.increaseTo(partitions)
        try {
            adminClient.createPartitions(mapOf(topicName to newPartitions)).all().get()
        } catch (expected: Exception) {
            logger.error(expected) { "Error while increasing partitions from topic: $topicName" }
            throw PrintMessage("Could not increase topic: $topicName partitions\n${expected.message}", error = true)
        }
    }

    fun updateTopicReplication(topicName: String, replicaCount: Int) {
        logger.info { "Updating topic: $topicName replica count to: $replicaCount" }
        val clusterNodes = getClusterNodes()
        val clusterTopicDescription = adminClient.describeTopics(listOf(topicName)).allTopicNames().get()

        val newReassignmentMap = createReassignmentMap(clusterTopicDescription, topicName, replicaCount, clusterNodes)

        try {
            adminClient.alterPartitionReassignments(newReassignmentMap).all().get()
        } catch (expected: Exception) {
            logger.error(expected) { "Error updating replication factor of topic $topicName" }
            throw PrintMessage(
                """
                Could not update replication factor for topic: $topicName
                ${expected.message}
                """.trimIndent(),
                error = true
            )
        }
    }

    private fun createReassignmentMap(
        clusterTopicDescription: MutableMap<String, TopicDescription>,
        topicName: String,
        replicaCount: Int,
        clusterNodes: Collection<Node>
    ): Map<TopicPartition, Optional<NewPartitionReassignment>> {
        val newReassignmentMap = clusterTopicDescription.map { (_, topicDescription) ->
            topicDescription.partitions().map { topicPartitionInfo ->
                val topicPartition = TopicPartition(topicName, topicPartitionInfo.partition())
                val replicas = topicPartitionInfo.replicas().map { it.id() }

                val newReplicas = if (replicas.size > replicaCount) {
                    replicas.subList(0, replicaCount)
                } else {
                    increaseReplicas(replicaCount, replicas, clusterNodes, topicName)
                }

                val partitionReassignment = NewPartitionReassignment(newReplicas)
                // Kafka library requires the value to be an optional
                topicPartition to Optional.of(partitionReassignment)
            }
        }.flatten().associate { it.first to it.second }
        return newReassignmentMap
    }

    private fun increaseReplicas(
        replicaCount: Int,
        replicas: List<Int>,
        clusterNodes: Collection<Node>,
        topicName: String
    ): List<Int> {
        val amountToAdd = replicaCount - replicas.size
        val clusterNodeIds = clusterNodes.map { it.id() }
        val nodesWithoutReplicas = clusterNodeIds - replicas
        if (nodesWithoutReplicas.isEmpty() || nodesWithoutReplicas.size < amountToAdd) {
            throw PrintMessage(
                """
                    Error while increasing replication factor for topic $topicName
                    Not enough nodes in the cluster
                """.trimIndent(),
                error = true
            )
        }
        val increasedReplicas = nodesWithoutReplicas.shuffled().subList(0, amountToAdd)
        return replicas + increasedReplicas
    }

    fun updateTopicConfig(topicName: String, configs: List<TopicConfigPlan>) {
        val configOps = configs.mapNotNull { topicConfigPlan ->
            val configEntry = ConfigEntry(topicConfigPlan.key, topicConfigPlan.newValue)
            when (topicConfigPlan.action) {
                PlanAction.ADD, PlanAction.UPDATE -> AlterConfigOp(configEntry, AlterConfigOp.OpType.SET)
                PlanAction.REMOVE -> AlterConfigOp(configEntry, AlterConfigOp.OpType.DELETE)
                else -> null
            }
        }

        val configResource = ConfigResource(ConfigResource.Type.TOPIC, topicName)
        try {
            adminClient.incrementalAlterConfigs(mapOf(configResource to configOps)).all().get()
        } catch (expected: Exception) {
            logger.error(expected) { "Error updating configuration of topic $topicName" }
            throw PrintMessage(
                """
                Error while applying configuration for topic: $topicName
                ${expected.message}
                """.trimIndent(),
                error = true
            )
        }
    }

    fun deleteTopic(topicName: String) {
        try {
            adminClient.deleteTopics(listOf(topicName)).all().get()
        } catch (expected: Exception) {
            logger.error(expected) { "Error removing topic $topicName" }
            throw PrintMessage(
                """
                Error while removing topic: $topicName
                ${expected.message}
                """.trimIndent(),
                error = true
            )
        }
    }

    private fun getClusterNodes(): Collection<Node> {
        return try {
            adminClient.describeCluster().nodes().get()
        } catch (expected: Exception) {
            logger.error(expected) { "Error while describing cluster" }
            throw PrintMessage("Could not get cluster nodes\n${expected.message}", error = true)
        }
    }
}
