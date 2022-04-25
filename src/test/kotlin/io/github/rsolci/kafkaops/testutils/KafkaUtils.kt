package io.github.rsolci.kafkaops.testutils

import io.github.rsolci.kafkaops.config.createKafkaAdminClient
import org.apache.kafka.clients.admin.TopicDescription

private val testAdminClient = createKafkaAdminClient()

fun getAllTopics(): MutableMap<String, TopicDescription> {
    val allTopics = testAdminClient.listTopics().names().get()
    return testAdminClient.describeTopics(allTopics).allTopicNames().get()
}

fun deleteAllTopics() {
    val topicNames = testAdminClient.listTopics().names().get()
    testAdminClient.deleteTopics(topicNames).all().get()
}
