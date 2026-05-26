package io.github.pylonmc.rebar.util.gui.unit

import net.kyori.adventure.text.Component

/**
 * Represents a prefix that a unit could have (grams, kilograms, nanograms, etc).
 *
 * Enum order is guaranteed to be the same as the SI unit prefixes, from largest to smallest.
 */
enum class MetricPrefix(
    /**
     * Scale, where `10^scale` is the multiplier of the unit. For example, for kilo, scale is 3, because 1 kilogram is 10^3 grams.
     */
    val scale: Int
) {
    QUETTA(30),
    RONNA(27),
    YOTTA(24),
    ZETTA(21),
    EXA(18),
    PETA(15),
    TERA(12),
    GIGA(9),
    MEGA(6),
    KILO(3),
    HECTO(2),
    DECA(1),
    NONE(0),
    DECI(-1),
    CENTI(-2),
    MILLI(-3),
    MICRO(-6),
    NANO(-9),
    PICO(-12),
    FEMTO(-15),
    ATTO(-18),
    ZEPTO(-21),
    YOCTO(-24),
    RONTO(-27),
    QUECTO(-30)
    ;

    val fullName: Component = Component.translatable("rebar.unit.prefix.${name.lowercase()}.name")
    val abbreviation: Component = Component.translatable("rebar.unit.prefix.${name.lowercase()}.abbr")

    companion object {
        /**
         * [HECTO], [DECA], and [DECI]
         */
        @JvmField
        val COMMONLY_UNUSED_PREFIXES = setOf(HECTO, DECA, DECI)
    }
}