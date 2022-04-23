package io.github.rsolci.kafkaops.printer

import com.github.ajalt.clikt.output.TermUi.echo
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
    echo("${27.toChar()}[${color.value}m", trailingNewline = false)
    echo(text, trailingNewline = false)
    echo("${27.toChar()}[${TextColor.DEFAULT.value}m", trailingNewline = true)
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
