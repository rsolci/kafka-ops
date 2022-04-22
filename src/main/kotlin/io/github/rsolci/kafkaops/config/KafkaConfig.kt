package io.github.rsolci.kafkaops.config

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.common.config.SaslConfigs

private const val USERNAME_ENV_VAR_NAME = "KAFKA_USERNAME"
private const val PASSWORD_ENV_VAR_NAME = "KAFKA_PASSWORD"
private const val ENV_VAR_PREFIX = "KAFKA_"

fun createKafkaAdminClient(): AdminClient {
    val config = createConfig()
    return KafkaAdminClient.create(config)
}

private fun createConfig(): Map<String, Any> {
    val environmentVariables = System.getenv()

    val username = environmentVariables[USERNAME_ENV_VAR_NAME]
    val password = environmentVariables[PASSWORD_ENV_VAR_NAME]

    val generalConfigs = environmentVariables.filter { entry ->
        entry.key.startsWith(ENV_VAR_PREFIX) &&
            entry.key != USERNAME_ENV_VAR_NAME && entry.key != PASSWORD_ENV_VAR_NAME
    }.entries.associate { entry ->
        entry.key.replace(ENV_VAR_PREFIX, "").replace("_", ".").lowercase() to entry.value
    }

    val loginConfig = createLoginConfig(username, password, generalConfigs)

    val commonConfig = createCommonConfig(generalConfigs)

    return generalConfigs + loginConfig + commonConfig
}

private fun createCommonConfig(generalConfigs: Map<String, String>): Map<String, String> {
    val commonConfig = if (!generalConfigs.contains(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
        mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092")
    } else emptyMap()
    return commonConfig
}

private fun createLoginConfig(
    username: String?,
    password: String?,
    generalConfigs: Map<String, String>
): Map<String, String> {
    val loginConfig = if (username != null && password != null) {
        val loginModule = when (generalConfigs[SaslConfigs.SASL_MECHANISM]) {
            "PLAIN" -> {
                "org.apache.kafka.common.security.plain.PlainLoginModule"
            }
            "SCRAM-SHA-256", "SCRAM-SHA-512" -> {
                "org.apache.kafka.common.security.scram.ScramLoginModule"
            }
            else -> {
                throw IllegalArgumentException("Wrong configuration KAFKA_SASL_MECHANISM")
            }
        }

        val escapedUser = username.escapeCredential()
        val escapedPass = password.escapeCredential()
        val jaasConfig = "$loginModule required username=\"$escapedUser\" password=\"$escapedPass\";"

        mapOf(SaslConfigs.SASL_JAAS_CONFIG to jaasConfig)
    } else if (username != null || password != null) {
        throw IllegalArgumentException("Missing configuration $USERNAME_ENV_VAR_NAME or $PASSWORD_ENV_VAR_NAME")
    } else {
        emptyMap()
    }
    return loginConfig
}

private fun String.escapeCredential(): String {
    return this.replace("\"", "\\\"")
}
