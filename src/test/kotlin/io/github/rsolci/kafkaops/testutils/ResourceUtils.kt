package io.github.rsolci.kafkaops.testutils

import java.io.File

fun String.asResourceFile(): File {
    val url = requireNotNull(ClassLoader.getSystemResource(this))
    return File(url.file)
}
