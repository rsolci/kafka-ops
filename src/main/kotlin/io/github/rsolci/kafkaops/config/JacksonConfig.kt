package io.github.rsolci.kafkaops.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule

fun createObjectMapper(): ObjectMapper {
    val objectMapper = ObjectMapper(YAMLFactory())
    objectMapper.registerModule(KotlinModule.Builder().build())
    objectMapper.enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
    objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    objectMapper.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
    return objectMapper
}

fun JsonMappingException.getInvalidFields(): List<String> {
    return this.path.map {
        it.fieldName
    }
}
