package io.github.rsolci.kafkaops.services

import mu.KotlinLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.TopicDescription

private val logger = KotlinLogging.logger { }

class KafkaService(
    private val adminClient: AdminClient
) {

    fun getTopics(): MutableMap<String, TopicDescription> {
        logger.info { "Listing topics from cluster" }
        val allTopics = adminClient.listTopics().names().get()
        return adminClient.describeTopics(allTopics).all().get()
    }
}
