package io.github.rsolci.kafkaops.printer

import io.github.rsolci.kafkaops.models.plan.PlanAction

fun green(text: String) {
    styleOutputColor(TextColor.GREEN, text)
}

fun yellow(text: String) {
    styleOutputColor(TextColor.YELLOW, text)
}

fun red(text: String) {
    styleOutputColor(TextColor.RED, text)
}

fun printAction(action: PlanAction, text: String) {
    when (action) {
        PlanAction.ADD -> green(text)
        PlanAction.UPDATE -> yellow(text)
        PlanAction.REMOVE -> red(text)
        PlanAction.DO_NOTHING -> styleOutputColor(TextColor.DEFAULT, text)
    }
}

private fun styleOutputColor(color: TextColor, text: String) {
    print("${27.toChar()}[${color.value}m")
    print(text)
    println("${27.toChar()}[${TextColor.DEFAULT.value}m")
}

@Suppress("MagicNumber")
private enum class TextColor(val value: Int) {
    DEFAULT(0),
    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37),
}
