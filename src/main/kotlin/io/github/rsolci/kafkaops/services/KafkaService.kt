package io.github.rsolci.kafkaops.services

import mu.KotlinLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ConfigEntry
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.common.config.ConfigResource
import java.util.concurrent.ExecutionException

private val logger = KotlinLogging.logger { }

class KafkaService(
    private val adminClient: AdminClient
) {

    fun getTopics(): MutableMap<String, TopicDescription> {
        logger.info { "Listing topics from cluster" }
        val allTopics = adminClient.listTopics().names().get()
        return adminClient.describeTopics(allTopics).all().get()
    }

    fun getConfigurationForTopics(topicNames: Set<String>): Map<String, List<ConfigEntry>> {
        val configResources = topicNames.map { ConfigResource(ConfigResource.Type.TOPIC, it) }
        val configResult = try {
            adminClient.describeConfigs(configResources).all().get()
        } catch (e: InterruptedException) {
            logger.error(e) { "Error while fetching topic configuration" }
            throw IllegalStateException("Could not load topic configuration", e)
        } catch (e: ExecutionException) {
            logger.error(e) { "Error while fetching topic configuration" }
            throw IllegalStateException("Could not load topic configuration", e)
        }

        return configResult.entries.associate { it.key.name() to it.value.entries().toList() }
    }
}
