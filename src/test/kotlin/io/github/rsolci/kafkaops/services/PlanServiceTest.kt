package io.github.rsolci.kafkaops.services

import io.github.rsolci.kafkaops.PlanService
import io.github.rsolci.kafkaops.config.createObjectMapper
import io.github.rsolci.kafkaops.models.plan.PlanAction
import io.github.rsolci.kafkaops.parsers.SchemaFileParser
import io.github.rsolci.kafkaops.testutils.asResourceFile
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.common.TopicPartitionInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class PlanServiceTest(
    @MockK private val kafkaServiceMock: KafkaService
) {
    private val planService = PlanService(
        kafkaService = kafkaServiceMock,
        schemaFileParser = SchemaFileParser(createObjectMapper())
    )

    @Test
    fun `should mark a topic that needs to be added and dont update a topic that dont need`() {
        val existingSchema = mutableMapOf(
            "noChangeTopic" to TopicDescription(
                "noChangeTopic",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk(), mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk(), mockk(), mockk()), mockk())
                )
            )
        )
        every { kafkaServiceMock.getTopics() } returns existingSchema

        val file = "schemas/new-topic.yaml".asResourceFile()

        val clusterPlan = planService.plan(file)

        assertEquals(2, clusterPlan.topicPlans.size)

        val topicPlanMap = clusterPlan.topicPlans.associateBy { it.name }
        val newTopic = checkNotNull(topicPlanMap["newTopic"])
        assertEquals(PlanAction.ADD, newTopic.action)

        assertEquals(PlanAction.ADD, newTopic.replicationPlan.action)
        assertEquals(8, newTopic.replicationPlan.newValue)
        assertNull(newTopic.replicationPlan.previousValue)

        assertEquals(PlanAction.ADD, newTopic.partitionPlan.action)
        assertNull(newTopic.partitionPlan.previousValue)
        assertEquals(3, newTopic.partitionPlan.newValue)

        val existingTopic = checkNotNull(topicPlanMap["noChangeTopic"])
        assertEquals(PlanAction.DO_NOTHING, existingTopic.action)

        assertEquals(PlanAction.DO_NOTHING, existingTopic.replicationPlan.action)
        assertEquals(4, existingTopic.replicationPlan.newValue)
        assertEquals(4, existingTopic.replicationPlan.previousValue)

        assertEquals(PlanAction.DO_NOTHING, existingTopic.partitionPlan.action)
        assertEquals(2, existingTopic.partitionPlan.newValue)
        assertEquals(2, existingTopic.partitionPlan.previousValue)
    }

    @Test
    fun `should mark a topic to update if the partition count increases`() {
        val existingSchema = mutableMapOf(
            "increasePartitions" to TopicDescription(
                "increasePartitions",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk())
                )
            )
        )
        every { kafkaServiceMock.getTopics() } returns existingSchema

        val file = "schemas/increase-partitions.yaml".asResourceFile()

        val clusterPlan = planService.plan(file)

        assertEquals(1, clusterPlan.topicPlans.size)

        val topicPlanMap = clusterPlan.topicPlans.associateBy { it.name }
        val newTopic = checkNotNull(topicPlanMap["increasePartitions"])
        assertEquals(PlanAction.UPDATE, newTopic.action)

        assertEquals(PlanAction.DO_NOTHING, newTopic.replicationPlan.action)
        assertEquals(2, newTopic.replicationPlan.newValue)
        assertEquals(2, newTopic.replicationPlan.previousValue)

        assertEquals(PlanAction.UPDATE, newTopic.partitionPlan.action)
        assertEquals(4, newTopic.partitionPlan.newValue)
        assertEquals(2, newTopic.partitionPlan.previousValue)
    }

    @Test
    fun `should mark a topic to update if the replication factor increases`() {
        val existingSchema = mutableMapOf(
            "increaseReplication" to TopicDescription(
                "increaseReplication",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk())
                )
            )
        )
        every { kafkaServiceMock.getTopics() } returns existingSchema

        val file = "schemas/increase-replication.yaml".asResourceFile()

        val clusterPlan = planService.plan(file)

        assertEquals(1, clusterPlan.topicPlans.size)

        val topicPlanMap = clusterPlan.topicPlans.associateBy { it.name }
        val newTopic = checkNotNull(topicPlanMap["increaseReplication"])
        assertEquals(PlanAction.UPDATE, newTopic.action)

        assertEquals(PlanAction.UPDATE, newTopic.replicationPlan.action)
        assertEquals(4, newTopic.replicationPlan.newValue)
        assertEquals(2, newTopic.replicationPlan.previousValue)

        assertEquals(PlanAction.DO_NOTHING, newTopic.partitionPlan.action)
        assertEquals(2, newTopic.partitionPlan.newValue)
        assertEquals(2, newTopic.partitionPlan.previousValue)
    }

    @Test
    fun `should not allow decrease in partitions`() {
        val existingSchema = mutableMapOf(
            "decreasePartitions" to TopicDescription(
                "decreasePartitions",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                )
            )
        )
        every { kafkaServiceMock.getTopics() } returns existingSchema

        val file = "schemas/decrease-partitions.yaml".asResourceFile()

        val exception = assertThrows<IllegalArgumentException> {
            planService.plan(file)
        }

        assertContains(exception.message!!, "Removing partitions is not yet supported")
    }

    @Test
    fun `should not mark topic to deletion if allow deletion flag is not true`() {
        val existingSchema = mutableMapOf(
            "decreasePartitions" to TopicDescription(
                "decreasePartitions",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                )
            ),
            "nonSchema" to TopicDescription(
                "nonSchema",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                )
            )
        )
        every { kafkaServiceMock.getTopics() } returns existingSchema

        val file = "schemas/decrease-partitions.yaml".asResourceFile()

        val clusterPlan = planService.plan(file)

        assertEquals(1, clusterPlan.topicPlans.size)

        val topicPlanMap = clusterPlan.topicPlans.associateBy { it.name }
        assertNull(topicPlanMap["nonSchema"])
    }

    @Test
    fun `should mark topic to deletion if allow deletion flag is true`() {
        val existingSchema = mutableMapOf(
            "decreasePartitions" to TopicDescription(
                "decreasePartitions",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                )
            ),
            "nonSchema" to TopicDescription(
                "nonSchema",
                false,
                listOf(
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                )
            )
        )
        every { kafkaServiceMock.getTopics() } returns existingSchema

        val file = "schemas/decrease-partitions.yaml".asResourceFile()

        val clusterPlan = planService.plan(file, true)

        assertEquals(2, clusterPlan.topicPlans.size)

        val topicPlanMap = clusterPlan.topicPlans.associateBy { it.name }
        val nonSchemaTopic = checkNotNull(topicPlanMap["nonSchema"])
        assertEquals(PlanAction.REMOVE, nonSchemaTopic.action)

        val existingTopic = checkNotNull(topicPlanMap["decreasePartitions"])
        assertEquals(PlanAction.DO_NOTHING, existingTopic.action)
    }
}
