package io.github.rsolci.kafkaops.parsers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.github.ajalt.clikt.core.FileNotFound
import com.github.ajalt.clikt.core.InvalidFileFormat
import com.github.ajalt.clikt.core.PrintMessage
import io.github.rsolci.kafkaops.config.getInvalidFields
import io.github.rsolci.kafkaops.models.schema.Schema
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val logger = KotlinLogging.logger { }

class SchemaFileParser(
    private val objectMapper: ObjectMapper
) {

    @Suppress("ThrowsCount", "SwallowedException")
    fun getSchema(inputFile: File?): Schema {
        val schemaFile = requireNotNull(inputFile) {
            "Schema file required"
        }

        return try {
            objectMapper.readValue(schemaFile, Schema::class.java)
        } catch (e: UnrecognizedPropertyException) {
            val invalidFields = e.getInvalidFields()
            logger.error(e) { "Invalid fields in schema file. Invalid fields: $invalidFields" }
            throw InvalidFileFormat(inputFile.name, "Invalid fields found while parsing schema file: $invalidFields")
        } catch (e: MismatchedInputException) {
            logger.error(e) { "Invalid fields in schema file" }
            throw InvalidFileFormat(inputFile.name, "Schema file is not valid")
        } catch (e: FileNotFoundException) {
            logger.error(e) { "Schema file not found" }
            throw FileNotFound(inputFile.name)
        } catch (e: IOException) {
            logger.error(e) { "Could not read schema file" }
            throw PrintMessage("Could not read from provided schema file")
        }
    }
}
