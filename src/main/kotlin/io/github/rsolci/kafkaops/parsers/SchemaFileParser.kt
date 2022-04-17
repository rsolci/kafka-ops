package io.github.rsolci.kafkaops.parsers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import io.github.rsolci.kafkaops.config.getInvalidFields
import io.github.rsolci.kafkaops.models.Schema
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class SchemaFileParser(
    private val objectMapper: ObjectMapper
) {

    @Suppress("ThrowsCount")
    fun getSchema(inputFile: File?): Schema {
        val schemaFile = requireNotNull(inputFile) {
            "Schema file required"
        }

        return try {
            objectMapper.readValue(schemaFile, Schema::class.java)
        } catch (e: UnrecognizedPropertyException) {
            val invalidFields = e.getInvalidFields()
            throw IllegalArgumentException("Invalid fields found while parsing schema file: $invalidFields", e)
        } catch (e: MismatchedInputException) {
            throw IllegalArgumentException("Schema file is not valid", e)
        } catch (e: FileNotFoundException) {
            throw IllegalArgumentException("Schema file not found", e)
        } catch (e: IOException) {
            throw IllegalArgumentException("Could not read from provided schema file", e)
        }
    }
}
