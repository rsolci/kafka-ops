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
import org.junit.jupiter.api.extension.ExtendWith
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
    fun `should compute a difference based on existing topics`() {
        val existingSchema = mutableMapOf(
            "noChangeTopic" to TopicDescription(
                "noChangeTopic",
                false,
                listOf(TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()),
                    TopicPartitionInfo(0, mockk(), listOf(mockk(), mockk()), mockk()))
            )
        )
        every { kafkaServiceMock.getTopics() } returns existingSchema

        val file = "schemas/full-schema.yaml".asResourceFile()

        val clusterPlan = planService.plan(file, false)

        assertEquals(4, clusterPlan.topicPlans.size)

        val topicPlanMap = clusterPlan.topicPlans.associateBy { it.name }
        val newTopic = checkNotNull(topicPlanMap["newTopic"])
        assertEquals(PlanAction.ADD, newTopic.action)

        assertEquals(PlanAction.ADD, newTopic.replicationPlan.action)
        assertEquals(8, newTopic.replicationPlan.newValue)
        assertNull(newTopic.replicationPlan.previousValue)

        assertEquals(PlanAction.ADD, newTopic.partitionPlan.action)
        assertNull(newTopic.partitionPlan.previousValue)
        assertEquals(3, newTopic.partitionPlan.newValue)
    }
}