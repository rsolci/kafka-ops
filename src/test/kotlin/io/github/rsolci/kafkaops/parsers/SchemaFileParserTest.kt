package io.github.rsolci.kafkaops.parsers

import io.github.rsolci.kafkaops.config.createObjectMapper
import io.github.rsolci.kafkaops.testutils.asResourceFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchemaFileParserTest {

    private val schemaFileParser = SchemaFileParser(createObjectMapper())

    @Test
    fun `null input file should not be parsed`() {
        val exception = assertThrows<IllegalArgumentException> {
            schemaFileParser.getSchema(null)
        }

        assertContains(exception.message!!, "Schema file required")
    }

    @Test
    fun `should use a existing file to parse`() {
        val file = File("non_existing.yaml")

        val exception = assertThrows<IllegalArgumentException> {
            schemaFileParser.getSchema(file)
        }

        assertContains(exception.message!!, "Schema file not found")
    }

    @Test
    fun `empty schema file should not be valid`() {
        val file = "schemas/empty.yaml".asResourceFile()

        val exception = assertThrows<IllegalArgumentException> {
            schemaFileParser.getSchema(file)
        }

        assertContains(exception.message!!, "not valid")
    }

    @Test
    fun `schema with unknown property should fail`() {
        val file = "schemas/invalid-property.yaml".asResourceFile()

        val exception = assertThrows<IllegalArgumentException> {
            schemaFileParser.getSchema(file)
        }

        assertContains(exception.message!!, "non-existing")
    }

    @Test
    fun `partition is required to define a topic`() {
        val file = "schemas/missing-topic-partition.yaml".asResourceFile()

        val exception = assertThrows<IllegalArgumentException> {
            schemaFileParser.getSchema(file)
        }

        assertContains(exception.message!!, "not valid")
    }

    @Test
    fun `replication is required to define a topic`() {
        val file = "schemas/missing-topic-replication.yaml".asResourceFile()

        val exception = assertThrows<IllegalArgumentException> {
            schemaFileParser.getSchema(file)
        }

        assertContains(exception.message!!, "not valid")
    }

    @Test
    fun `schema containing only topics is a valid file`() {
        val file = "schemas/only-topics.yaml".asResourceFile()

        val schema = schemaFileParser.getSchema(file)

        val topic = schema.topics["newTopic"]
        assertNotNull(topic)
        assertEquals(3, topic.partitions)
        assertEquals(8, topic.replication)
        val retention = topic.config["retention.ms"]
        assertNotNull(retention)
        assertEquals("100000", retention)
    }

    @Test
    fun `should correctly parse a complete schema`() {
        val file = "schemas/valid.yaml".asResourceFile()

        val schema = schemaFileParser.getSchema(file)

        val topic = schema.topics["newTopic"]
        assertNotNull(topic)
        assertEquals(3, topic.partitions)
        assertEquals(8, topic.replication)
        val retention = topic.config["retention.ms"]
        assertNotNull(retention)
        assertEquals("100000", retention)

        val settings = schema.settings
        assertNotNull(settings)
        assertTrue { settings.topics.ignoreList.containsAll(listOf("topic1", "prefix1")) }
    }
}
