package io.github.rsolci.kafkaops.extensions

fun String.anyMatch(regexList: Iterable<Regex>, trueFunction: (() -> Unit)? = null): Boolean {
    val matchesAnyRegex = regexList.map { it.matches(this) }.any { it }
    if (matchesAnyRegex) {
        trueFunction?.invoke()
    }
    return matchesAnyRegex
}
