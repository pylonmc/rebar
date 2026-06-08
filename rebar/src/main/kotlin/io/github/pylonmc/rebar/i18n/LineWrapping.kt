package io.github.pylonmc.rebar.i18n

import com.google.common.base.Preconditions
import io.github.pylonmc.rebar.config.RebarConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

/**
 * The root component which has all the other components in
 * a line as children
 */
@JvmSynthetic
internal val DEFAULT_COMPONENT = Component.empty()
    .decoration(TextDecoration.ITALIC, false)
    .color(NamedTextColor.GRAY)

/**
 * Splits a [Component] into multiple components with `\n` as a delimeter.
 *
 * Only considers newlines inside [TextComponent]s
 */
@JvmSynthetic
internal fun splitByNewlines(component: Component): List<Component> {
    val lines = mutableListOf<Component>()
    val currentLine = DEFAULT_COMPONENT
    lines.add(splitByNewlines(lines, currentLine, component.style(), component))
    return lines
}

private fun splitByNewlines(lines: MutableList<Component>, currentLine: Component, style: Style, component: Component): Component {
    var currentLine = currentLine

    // Handle component contents
    if (component is TextComponent) {
        // Text component could have a newline or multiple newlines in it
        val split = component.content().split('\n')
        Preconditions.checkState(!split.isEmpty())
        for (text in split.dropLast(1)) {
            currentLine = currentLine.append(Component.text(text).style(style))
            lines.add(currentLine)
            currentLine = DEFAULT_COMPONENT
        }
        currentLine = currentLine.append(Component.text(split.last()).style(style))
    } else {
        // Add JUST the component contents, and not any of its children
        currentLine = currentLine.append(component.children(mutableListOf<Component>()))
    }

    // Handle children
    for (child in component.children()) {
        currentLine = splitByNewlines(lines, currentLine, style.merge(child.style()), child)
    }

    return currentLine
}

/**
 * Splits a single [Component] into multiple lines, aiming to limit each line's
 * length to [RebarConfig.TRANSLATION_WRAP_LIMIT]
 *
 * Only considers newlines inside [TextComponent]s
 */
@JvmSynthetic
internal fun wrapLine(component: Component): List<Component> {
    val lines = mutableListOf<Component>()
    val currentLine = DEFAULT_COMPONENT
    val currentLineLength = 0
    val result = wrapLine(lines, currentLine, currentLineLength, component.style(), component)
    lines.add(result.first)
    return lines
}

private fun wrapLine(
    lines: MutableList<Component>,
    currentLine: Component,
    currentLineLength: Int,
    style: Style,
    component: Component
): Pair<Component, Int> {
    var currentLine = currentLine
    var currentLineLength = currentLineLength

    // Handle component contents
    if (component is TextComponent) {
        // Text component has stuff (text) that we can split up. Other types of components don't.
        // So line wrapping might be wrong for certain components (e.g. translateable components
        // which are handled clientside) but this is fine, it can't be perfect.
        var content = component.content()
        while (currentLineLength + content.length > RebarConfig.TRANSLATION_WRAP_LIMIT) {
            // How many more characters fit on the current line
            val remaining = RebarConfig.TRANSLATION_WRAP_LIMIT - currentLineLength

            // Current line is already full — flush it and restart on a fresh line
            if (remaining <= 0) {
                lines.add(currentLine)
                currentLine = DEFAULT_COMPONENT
                currentLineLength = 0
                continue
            }

            // Try to snap to the end of a word so we don't cut words in half.
            // (content.length > remaining here, so content[endIndex] is always in bounds.)
            var endIndex = remaining
            while (endIndex > 0 && content[endIndex] != ' ') {
                endIndex -= 1
            }

            // The amount of text to put on this line, and whether to skip a delimiter space.
            val cut: Int
            val skipDelimiter: Boolean
            if (endIndex == 0) {
                // No space within the limit: either a single word longer than the wrap
                // limit, or a language without spaces (e.g. Chinese/Japanese). Hard-break
                // at the limit so we keep making progress instead of looping forever and
                // silently dropping characters.
                cut = remaining
                skipDelimiter = false
            } else {
                cut = endIndex
                skipDelimiter = true
            }

            currentLine = currentLine.append(Component.text(content.substring(0, cut)).style(style))
            lines.add(currentLine)
            currentLine = DEFAULT_COMPONENT
            currentLineLength = 0
            content = content.substring(if (skipDelimiter) cut + 1 else cut)
        }
        currentLine = currentLine.append(Component.text(content).style(style))
        currentLineLength += content.length

    } else {
        // Treat as zero-length
        // Add JUST the component contents, and not any of its children
        currentLine = currentLine.append(component.children(mutableListOf<Component>()))
    }

    // Handle children
    for (child in component.children()) {
        val result = wrapLine(lines, currentLine, currentLineLength, style.merge(child.style()), child)
        currentLine = result.first
        currentLineLength = result.second
    }

    return Pair(currentLine, currentLineLength)
}
