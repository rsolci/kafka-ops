package io.github.rsolci.kafkaops

import io.github.rsolci.kafkaops.testutils.asResourceFile
import io.github.rsolci.kafkaops.testutils.deleteAllTopics
import io.github.rsolci.kafkaops.testutils.getAllTopics
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.SetEnvironmentVariable
import org.junitpioneer.jupiter.SetEnvironmentVariable.SetEnvironmentVariables
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        val topics = getAllTopics()
        val topicName = "newTopic"
        val createdTopic = topics[topicName]
        assertNotNull(createdTopic)
        assertEquals(topicName, createdTopic.name())
        assertEquals(1, createdTopic.partitions().size)
        assertEquals(1, createdTopic.partitions().first().replicas().size)
    }

    @Test
    fun `should increase partitions of an existing topic`() {
        apply("schemas/integration/new-topic.yaml")

        apply("schemas/integration/increase-partitions.yaml")

        val topics = getAllTopics()
        val topicName = "newTopic"
        val createdTopic = topics[topicName]
        assertNotNull(createdTopic)
        assertEquals(topicName, createdTopic.name())
        assertEquals(2, createdTopic.partitions().size)
        assertEquals(1, createdTopic.partitions().first().replicas().size)
    }

    private fun apply(schemaFile: String) {
        val completePath = schemaFile.asResourceFile().absolutePath
        main(arrayOf("-s", completePath, "-d", "apply"))
    }
}
