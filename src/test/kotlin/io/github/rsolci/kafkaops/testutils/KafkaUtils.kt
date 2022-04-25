package io.github.rsolci.kafkaops.testutils

import io.github.rsolci.kafkaops.config.createKafkaAdminClient
import io.github.rsolci.kafkaops.services.KafkaService
import org.apache.kafka.clients.admin.ConfigEntry
import org.apache.kafka.clients.admin.TopicDescription
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import java.time.Duration

private val testAdminClient = createKafkaAdminClient()

fun getAllTopics(): MutableMap<String, TopicDescription> {
    val allTopics = testAdminClient.listTopics().names().get()
    return testAdminClient.describeTopics(allTopics).allTopicNames().get()
}

fun deleteAllTopics() {
    val topicNames = testAdminClient.listTopics().names().get()
    testAdminClient.deleteTopics(topicNames).all().get()
    await.atMost(Duration.ofSeconds(5)) until {
        testAdminClient.listTopics().names().get().isEmpty()
    }
}

fun getTopicConfigs(topicName: String): Map<String, List<ConfigEntry>> {
    return KafkaService(testAdminClient).getConfigurationForTopics(setOf(topicName))
}
