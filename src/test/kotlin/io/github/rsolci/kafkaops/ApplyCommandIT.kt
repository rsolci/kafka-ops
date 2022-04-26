package io.github.rsolci.kafkaops

import io.github.rsolci.kafkaops.testutils.asResourceFile
import io.github.rsolci.kafkaops.testutils.deleteAllTopics
import io.github.rsolci.kafkaops.testutils.getAllTopics
import io.github.rsolci.kafkaops.testutils.getTopicConfigs
import org.apache.kafka.clients.admin.ConfigEntry
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.SetEnvironmentVariable
import org.junitpioneer.jupiter.SetEnvironmentVariable.SetEnvironmentVariables
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SetEnvironmentVariables(
    SetEnvironmentVariable(key = "KAFKA_BOOTSTRAP_SERVERS", value = "localhost:29092"),
    SetEnvironmentVariable(key = "KAFKA_USERNAME", value = "test"),
    SetEnvironmentVariable(key = "KAFKA_PASSWORD", value = "test-secret"),
    SetEnvironmentVariable(key = "KAFKA_SASL_MECHANISM", value = "PLAIN"),
    SetEnvironmentVariable(key = "KAFKA_SECURITY_PROTOCOL", value = "SASL_PLAINTEXT"),
)
class ApplyCommandIT {

    @BeforeEach
    fun setUp() {
        deleteAllTopics()
    }

    @Test
    fun `should add a new topic to the cluster`() {
        apply("schemas/integration/new-topic.yaml")
        val topicName = "newTopic"
        await.atMost(Duration.ofSeconds(1)) untilAsserted {
            val topics = getAllTopics()
            val createdTopic = topics[topicName]
            assertNotNull(createdTopic)
            assertEquals(topicName, createdTopic.name())
            assertEquals(1, createdTopic.partitions().size)
            assertEquals(1, createdTopic.partitions().first().replicas().size)
        }
    }

    @Test
    fun `should increase partitions of an existing topic`() {
        apply("schemas/integration/new-topic.yaml")

        apply("schemas/integration/increase-partitions.yaml")
        val topicName = "newTopic"

        await.atMost(Duration.ofSeconds(1)) untilAsserted {
            val topics = getAllTopics()
            val createdTopic = topics[topicName]
            assertNotNull(createdTopic)
            assertEquals(topicName, createdTopic.name())
            assertEquals(2, createdTopic.partitions().size)
            assertEquals(1, createdTopic.partitions().first().replicas().size)
            assertEquals(1, createdTopic.partitions().last().replicas().size)
        }
    }

    @Test
    fun `should increase replication of an existing topic`() {
        apply("schemas/integration/new-topic.yaml")

        apply("schemas/integration/increase-replication.yaml")

        await.atMost(Duration.ofSeconds(1)) untilAsserted {
            val topics = getAllTopics()
            val topicName = "newTopic"
            val createdTopic = topics[topicName]
            assertNotNull(createdTopic)
            assertEquals(topicName, createdTopic.name())
            assertEquals(1, createdTopic.partitions().size)
            assertEquals(2, createdTopic.partitions().first().replicas().size)
        }
    }

    @Test
    fun `should update configuration of an existing topic`() {
        apply("schemas/integration/new-topic.yaml")

        apply("schemas/integration/increase-config.yaml")
        val topicName = "newTopic"

        await.atMost(Duration.ofSeconds(1)) untilAsserted {
            val topics = getTopicConfigs(topicName)
            val topicConfigs = topics[topicName]
            assertNotNull(topicConfigs)
            val retentionConfig = topicConfigs.find { it.name() == "retention.ms" }
            assertNotNull(retentionConfig)
            assertEquals("200000", retentionConfig.value())
        }
    }

    @Test
    fun `should delete configuration of an existing topic`() {
        apply("schemas/integration/new-topic.yaml")

        apply("schemas/integration/without-config.yaml")
        val topicName = "newTopic"

        await.atMost(Duration.ofSeconds(1)) untilAsserted {
            val topics = getTopicConfigs(topicName)
            val topicConfigs = topics[topicName]
            assertNotNull(topicConfigs)
            val retentionConfig =
                topicConfigs.find {
                    it.name() == "retention.ms" &&
                        it.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG
                }
            assertNull(retentionConfig)
        }
    }

    @Test
    fun `should delete a topic if not present on schema`() {
        val topicName = "newTopic"
        val toBeDeletedTopic = "secondTopic"
        apply("schemas/integration/two-topics.yaml")

        await.atMost(Duration.ofSeconds(1)) untilAsserted {
            val topics = getAllTopics()
            assertEquals(2, topics.size)
            assertNotNull(topics[topicName])
            assertNotNull(topics[toBeDeletedTopic])
        }

        apply("schemas/integration/new-topic.yaml")

        await.atMost(Duration.ofSeconds(1)) untilAsserted {
            val topics = getAllTopics()
            assertEquals(1, topics.size)
            assertNotNull(topics[topicName])
        }
    }

    private fun apply(schemaFile: String) {
        val completePath = schemaFile.asResourceFile().absolutePath
        main(arrayOf("-s", completePath, "-d", "-v", "apply"))
    }
}
