package io.github.rsolci.kafkaops.services

import io.github.rsolci.kafkaops.models.plan.ClusterPlan
import io.github.rsolci.kafkaops.models.plan.PartitionPlan
import io.github.rsolci.kafkaops.models.plan.PlanAction
import io.github.rsolci.kafkaops.models.plan.ReplicationPlan
import io.github.rsolci.kafkaops.models.plan.TopicConfigPlan
import io.github.rsolci.kafkaops.models.plan.TopicPlan
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class ApplyServiceTest(
    @MockK private val kafkaServiceMock: KafkaService
) {

    private val applyService = ApplyService(kafkaServiceMock)

    @Test
    fun `by default should not call delete even if the plan contains delete actions`() {
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = "deleted",
                    action = PlanAction.REMOVE,
                    partitionPlan = PartitionPlan(0, 0, PlanAction.REMOVE),
                    replicationPlan = ReplicationPlan(0, 0, PlanAction.REMOVE)
                )
            )
        )

        applyService.apply(clusterPlan)

        verify(exactly = 0) { kafkaServiceMock.deleteTopic(any()) }
    }

    @Test
    fun `should not call delete if the plan dont contains delete actions`() {
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = "deleted",
                    action = PlanAction.ADD,
                    partitionPlan = PartitionPlan(0, 0, PlanAction.REMOVE),
                    replicationPlan = ReplicationPlan(0, 0, PlanAction.REMOVE)
                )
            )
        )
        justRun { kafkaServiceMock.createTopic(any(), any(), any(), any()) }

        applyService.apply(clusterPlan, true)

        verify(exactly = 0) { kafkaServiceMock.deleteTopic(any()) }
    }

    @Test
    fun `if specified should call delete when the plan contains delete actions`() {
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = "deleted",
                    action = PlanAction.REMOVE,
                    partitionPlan = PartitionPlan(0, 0, PlanAction.REMOVE),
                    replicationPlan = ReplicationPlan(0, 0, PlanAction.REMOVE)
                )
            )
        )

        justRun { kafkaServiceMock.deleteTopic(any()) }

        applyService.apply(clusterPlan, true)

        verify(exactly = 1) { kafkaServiceMock.deleteTopic(eq("deleted")) }
    }

    @Test
    fun `should create topics with the desired configuration`() {
        val topicName = "added"
        val partitionValue = 1
        val replicationValue = 2
        val config1 = TopicConfigPlan("config1", "value1", null, PlanAction.ADD)
        val config2 = TopicConfigPlan("config2", "value2", null, PlanAction.ADD)
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = topicName,
                    action = PlanAction.ADD,
                    partitionPlan = PartitionPlan(0, partitionValue, PlanAction.ADD),
                    replicationPlan = ReplicationPlan(0, replicationValue, PlanAction.ADD),
                    topicConfigPlans = listOf(config1, config2)
                )
            )
        )
        justRun { kafkaServiceMock.createTopic(any(), any(), any(), any()) }

        applyService.apply(clusterPlan)

        val configPlansSlot = slot<Map<String, String>>()

        verify {
            kafkaServiceMock.createTopic(
                eq(topicName),
                eq(partitionValue),
                eq(replicationValue.toShort()),
                capture(configPlansSlot)
            )
        }

        val configMap = configPlansSlot.captured
        assertEquals(2, configMap.size)
        assertEquals(config1.newValue, configMap[config1.key])
        assertEquals(config2.newValue, configMap[config2.key])
    }

    @Test
    fun `should update all parameters`() {
        val topicName = "updated"
        val partitionValue = 2
        val replicationValue = 3
        val config1 = TopicConfigPlan("config1", "value1", null, PlanAction.UPDATE)
        val config2 = TopicConfigPlan("config2", "value2", null, PlanAction.UPDATE)
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = topicName,
                    action = PlanAction.UPDATE,
                    partitionPlan = PartitionPlan(0, partitionValue, PlanAction.UPDATE),
                    replicationPlan = ReplicationPlan(0, replicationValue, PlanAction.UPDATE),
                    topicConfigPlans = listOf(config1, config2)
                )
            )
        )

        justRun { kafkaServiceMock.increaseTopicPartitions(any(), any()) }
        justRun { kafkaServiceMock.updateTopicReplication(any(), any()) }
        justRun { kafkaServiceMock.updateTopicConfig(any(), any()) }

        applyService.apply(clusterPlan)

        verify(exactly = 1) { kafkaServiceMock.increaseTopicPartitions(eq(topicName), eq(partitionValue)) }
        verify(exactly = 1) { kafkaServiceMock.updateTopicReplication(eq(topicName), eq(replicationValue)) }
        verify(exactly = 1) { kafkaServiceMock.updateTopicConfig(eq(topicName), eq(listOf(config1, config2))) }
    }

    @Test
    fun `should not update partition if not required`() {
        val topicName = "updated"
        val partitionValue = 2
        val replicationValue = 3
        val config1 = TopicConfigPlan("config1", "value1", null, PlanAction.UPDATE)
        val config2 = TopicConfigPlan("config2", "value2", null, PlanAction.UPDATE)
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = topicName,
                    action = PlanAction.UPDATE,
                    partitionPlan = PartitionPlan(0, partitionValue, PlanAction.DO_NOTHING),
                    replicationPlan = ReplicationPlan(0, replicationValue, PlanAction.UPDATE),
                    topicConfigPlans = listOf(config1, config2)
                )
            )
        )

        justRun { kafkaServiceMock.increaseTopicPartitions(any(), any()) }
        justRun { kafkaServiceMock.updateTopicReplication(any(), any()) }
        justRun { kafkaServiceMock.updateTopicConfig(any(), any()) }

        applyService.apply(clusterPlan)

        verify(exactly = 0) { kafkaServiceMock.increaseTopicPartitions(eq(topicName), eq(partitionValue)) }
        verify(exactly = 1) { kafkaServiceMock.updateTopicReplication(eq(topicName), eq(replicationValue)) }
        verify(exactly = 1) { kafkaServiceMock.updateTopicConfig(eq(topicName), eq(listOf(config1, config2))) }
    }

    @Test
    fun `should not update replication if not required`() {
        val topicName = "updated"
        val partitionValue = 2
        val replicationValue = 3
        val config1 = TopicConfigPlan("config1", "value1", null, PlanAction.UPDATE)
        val config2 = TopicConfigPlan("config2", "value2", null, PlanAction.UPDATE)
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = topicName,
                    action = PlanAction.UPDATE,
                    partitionPlan = PartitionPlan(0, partitionValue, PlanAction.UPDATE),
                    replicationPlan = ReplicationPlan(0, replicationValue, PlanAction.DO_NOTHING),
                    topicConfigPlans = listOf(config1, config2)
                )
            )
        )

        justRun { kafkaServiceMock.increaseTopicPartitions(any(), any()) }
        justRun { kafkaServiceMock.updateTopicReplication(any(), any()) }
        justRun { kafkaServiceMock.updateTopicConfig(any(), any()) }

        applyService.apply(clusterPlan)

        verify(exactly = 1) { kafkaServiceMock.increaseTopicPartitions(eq(topicName), eq(partitionValue)) }
        verify(exactly = 0) { kafkaServiceMock.updateTopicReplication(eq(topicName), eq(replicationValue)) }
        verify(exactly = 1) { kafkaServiceMock.updateTopicConfig(eq(topicName), eq(listOf(config1, config2))) }
    }

    @Test
    fun `should not update config if the list is empty`() {
        val topicName = "updated"
        val partitionValue = 2
        val replicationValue = 3
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = topicName,
                    action = PlanAction.UPDATE,
                    partitionPlan = PartitionPlan(0, partitionValue, PlanAction.UPDATE),
                    replicationPlan = ReplicationPlan(0, replicationValue, PlanAction.UPDATE),
                    topicConfigPlans = listOf()
                )
            )
        )

        justRun { kafkaServiceMock.increaseTopicPartitions(any(), any()) }
        justRun { kafkaServiceMock.updateTopicReplication(any(), any()) }
        justRun { kafkaServiceMock.updateTopicConfig(any(), any()) }

        applyService.apply(clusterPlan)

        verify(exactly = 1) { kafkaServiceMock.increaseTopicPartitions(eq(topicName), eq(partitionValue)) }
        verify(exactly = 1) { kafkaServiceMock.updateTopicReplication(eq(topicName), eq(replicationValue)) }
        verify(exactly = 0) { kafkaServiceMock.updateTopicConfig(eq(topicName), any()) }
    }

    @Test
    fun `should not update config if there is no changes in configuration`() {
        val topicName = "updated"
        val partitionValue = 2
        val replicationValue = 3
        val config1 = TopicConfigPlan("config1", "value1", null, PlanAction.DO_NOTHING)
        val config2 = TopicConfigPlan("config2", "value2", null, PlanAction.DO_NOTHING)
        val clusterPlan = ClusterPlan(
            topicPlans = listOf(
                TopicPlan(
                    name = topicName,
                    action = PlanAction.UPDATE,
                    partitionPlan = PartitionPlan(0, partitionValue, PlanAction.UPDATE),
                    replicationPlan = ReplicationPlan(0, replicationValue, PlanAction.UPDATE),
                    topicConfigPlans = listOf(config1, config2)
                )
            )
        )

        justRun { kafkaServiceMock.increaseTopicPartitions(any(), any()) }
        justRun { kafkaServiceMock.updateTopicReplication(any(), any()) }
        justRun { kafkaServiceMock.updateTopicConfig(any(), any()) }

        applyService.apply(clusterPlan)

        verify(exactly = 1) { kafkaServiceMock.increaseTopicPartitions(eq(topicName), eq(partitionValue)) }
        verify(exactly = 1) { kafkaServiceMock.updateTopicReplication(eq(topicName), eq(replicationValue)) }
        verify(exactly = 0) { kafkaServiceMock.updateTopicConfig(eq(topicName), any()) }
    }
}
