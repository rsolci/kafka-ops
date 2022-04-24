package io.github.rsolci.kafkaops.services

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.Config
import org.apache.kafka.clients.admin.ConfigEntry
import org.apache.kafka.clients.admin.CreateTopicsResult
import org.apache.kafka.clients.admin.DescribeConfigsResult
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.config.ConfigResource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class KafkaServiceTest(
    @MockK private val adminClientMock: AdminClient
) {
    private val kafkaService = KafkaService(adminClientMock)

    @Test
    fun `getConfigurationForTopics should describe configs for topic resources only`() {
        val kafkaFutureMock = mockk<KafkaFuture<Map<ConfigResource, Config>>>()
        every { kafkaFutureMock.get() } returns mapOf(
            ConfigResource(ConfigResource.Type.TOPIC, "topic1") to Config(
                listOf(
                    ConfigEntry("c1", "v1")
                )
            ),
            ConfigResource(ConfigResource.Type.TOPIC, "topic2") to Config(
                listOf(
                    ConfigEntry("c2", "v2")
                )
            )
        )
        val describeResultMock = mockk<DescribeConfigsResult>()
        every { describeResultMock.all() } returns kafkaFutureMock
        every { adminClientMock.describeConfigs(any()) } returns describeResultMock

        val configMap = kafkaService.getConfigurationForTopics(setOf("topic1", "topic2"))

        val configResourceSlot = slot<List<ConfigResource>>()
        verify(exactly = 1) { adminClientMock.describeConfigs(capture(configResourceSlot)) }

        val configResources = configResourceSlot.captured
        assertEquals(2, configResources.size)
        configResources.any { it.name() == "topic1" && it.type() == ConfigResource.Type.TOPIC }
        configResources.any { it.name() == "topic2" && it.type() == ConfigResource.Type.TOPIC }

        assertEquals(2, configMap.size)
        assertEquals(1, configMap["topic1"]?.size)
        assertEquals("c1", configMap["topic1"]?.get(0)?.name())
        assertEquals("v1", configMap["topic1"]?.get(0)?.value())

        assertEquals(1, configMap["topic2"]?.size)
        assertEquals("c2", configMap["topic2"]?.get(0)?.name())
        assertEquals("v2", configMap["topic2"]?.get(0)?.value())
    }

    @Test
    fun `create topic should carry all configurations to admin client`() {
        val kafkaFutureMock = mockk<KafkaFuture<Void>>()
        justRun { kafkaFutureMock.get() }
        val createTopicsResultMockK = mockk<CreateTopicsResult>()
        every { createTopicsResultMockK.all() } returns kafkaFutureMock
        every { adminClientMock.createTopics(any()) } returns createTopicsResultMockK

        val configs = mapOf(
            "c1" to "v1",
            "c2" to "v2",
        )
        kafkaService.createTopic(
            "topic1", 2, 3, configs
        )

        val newTopicSlot = slot<List<NewTopic>>()
        verify(exactly = 1) { adminClientMock.createTopics(capture(newTopicSlot)) }

        val newTopics = newTopicSlot.captured
        assertEquals(1, newTopics.size)
        val newTopic = newTopics.first()
        assertEquals("topic1", newTopic.name())
        assertEquals(2, newTopic.numPartitions())
        assertEquals(3, newTopic.replicationFactor())
        assertEquals(configs, newTopic.configs())
    }
}
