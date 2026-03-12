package io.github.pylonmc.rebar.util.gui.unit

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Duration
import java.util.EnumSet

/**
 * Handles formatting of a specific unit. Call [format] to format a value using this unit.
 *
 * @param name The English name of the unit (for example 'kilograms')
 * @param singular A component representing the long singular form of this unit (kilogram, meter, liter, etc)
 * @param plural A component representing the long plural form of this unit (kilograms, meters, liters, etc)
 * @param abbreviation A component representing the abbreviated form of this unit (kg, m, L, etc)
 * @param defaultPrefix The prefix (kilo, nano, etc) used for this unit unless specified while formatting.
 * For example, if you create a 'grams' unit and specify 'kilo' as the default prefix, calling [format] with
 * 100 will return '100 kilograms'
 * @param defaultStyle The style to apply to the unit (not the value)
 * to the output.
 */
class UnitFormat @JvmOverloads constructor(
    val name: String,
    val singular: Component,
    val plural: Component,
    val abbreviation: Component? = null,
    val defaultPrefix: MetricPrefix = MetricPrefix.NONE,
    val defaultStyle: Style = Style.empty(),
) {

    private constructor(
        name: String,
        color: TextColor,
        abbreviate: Boolean,
        prefix: MetricPrefix = MetricPrefix.NONE,
    ) : this(
        name = name,
        singular = Component.translatable("rebar.unit.$name.singular"),
        plural = Component.translatable("rebar.unit.$name.plural"),
        abbreviation = Component.translatable("rebar.unit.$name.abbr").takeIf { abbreviate },
        defaultPrefix = prefix,
        defaultStyle = Style.style(color),
    )

    init {
        allUnits[name] = this
    }

    fun format(value: BigDecimal) = Formatted(value.stripTrailingZeros())

    fun format(value: Int) = format(BigDecimal.valueOf(value.toLong()))

    fun format(value: Long) = format(BigDecimal.valueOf(value))

    fun format(value: Float): Formatted {
        check(!value.isNaN() && !value.isInfinite()) { "Cannot format NaN or infinite values" }
        return format(BigDecimal.valueOf(value.toDouble()))
    }

    fun format(value: Double): Formatted {
        check(!value.isNaN() && !value.isInfinite()) { "Cannot format NaN or infinite values" }
        return format(BigDecimal.valueOf(value))
    }

    /**
     * Represents a value that has already been formatted.
     *
     * You can use this class to override how an already-formatted value is displayed.
     */
    inner class Formatted internal constructor(private val value: BigDecimal) : ComponentLike {
        private var sigFigs = value.precision()
        private var decimalPlaces = value.scale()
        private var forceDecimalPlaces = false
        private var abbreviate = true
        private var unitStyle = defaultStyle
        private var prefix: MetricPrefix? = defaultPrefix
        private val badPrefixes = EnumSet.noneOf(MetricPrefix::class.java)

        /**
         * Sets the number of significant figures. For example, if this is set to 3, then a value
         * of 0.472894 will be shown as 0.473.
         */
        fun significantFigures(sigFigs: Int) = apply { this.sigFigs = sigFigs }

        /**
         * Sets the number of decimal places. This overrides significant figures if both are set.
         */
        fun decimalPlaces(decimalPlaces: Int) = apply { this.decimalPlaces = decimalPlaces }

        /**
         * Sets whether a value should always have decimal places. For example, if set to true, then
         * the value 145 will be displayed as '145.0'
         *
         * This overrides decimal places if both are set.
         */
        fun forceDecimalPlaces(force: Boolean) = apply { this.forceDecimalPlaces = force }

        /**
         * Sets whether the abbreviation should be used instead of the full name.
         */
        fun abbreviate(abbreviate: Boolean) = apply { this.abbreviate = abbreviate }

        /**
         * Overrides the style of the unit.
         */
        fun unitStyle(style: Style) = apply { this.unitStyle = style }

        /**
         * Overrides the default prefix (and adjusts the value shown accordingly).
         */
        fun prefix(prefix: MetricPrefix) = apply { this.prefix = prefix }

        /**
         * Sets what prefixes should not be used.
         */
        fun ignorePrefixes(prefixes: Collection<MetricPrefix>) = apply { badPrefixes.addAll(prefixes) }

        /**
         * Sets what prefixes should not be used.
         */
        fun ignorePrefixes(vararg prefixes: MetricPrefix) = apply { badPrefixes.addAll(prefixes) }

        /**
         * Sets whether the prefix should be automatically selected instead of using the default
         * prefix (if set).
         */
        fun autoSelectPrefix() = apply { prefix = null }

        /**
         * Builds a component representing the value and unit.
         */
        override fun asComponent(): Component {
            var usedValue = value.round(MathContext(sigFigs, RoundingMode.HALF_UP))
            usedValue = usedValue.setScale(decimalPlaces, RoundingMode.HALF_UP)
            if (!forceDecimalPlaces) {
                usedValue = usedValue.stripTrailingZeros()
            }

            var usedPrefix = if (prefix == null) {
                val exponent = value.precision() - value.scale() - if (value.signum() == 0) 0 else 1
                val prefix = MetricPrefix.entries.firstOrNull { it.scale <= exponent }
                prefix ?: defaultPrefix
            } else {
                prefix!!
            }
            while (usedPrefix in badPrefixes) {
                usedPrefix = MetricPrefix.entries[MetricPrefix.entries.indexOf(usedPrefix) + 1]
            }

            usedValue = usedValue.movePointLeft(usedPrefix.scale - defaultPrefix.scale)

            val number = Component.text(usedValue.toPlainString())
            var unit = Component.empty().style(unitStyle)
            unit = if (abbreviate && abbreviation != null) {
                unit
                    .append(usedPrefix.abbreviation)
                    .append(abbreviation)
            } else {
                unit
                    .append(usedPrefix.fullName)
                    .append(if (usedValue == BigDecimal.ONE) singular else plural)
            }

            return number.append(Component.text(" ")).append(unit)
        }
    }

    companion object {

        @JvmSynthetic
        internal val allUnits = mutableMapOf<String, UnitFormat>()

        @JvmField
        val BLOCKS = UnitFormat(
            "blocks",
            TextColor.color(0x1eaa56),
            abbreviate = false
        )

        @JvmField
        val BLOCKS_PER_SECOND = UnitFormat(
            "blocks_per_second",
            TextColor.color(0x0ae256),
            abbreviate = true
        )

        @JvmField
        val CHUNKS = UnitFormat(
            "chunks",
            TextColor.color(0x136D37),
            abbreviate = false
        )

        @JvmField
        val HEARTS = UnitFormat("hearts", TextColor.color(0xdb3b43), abbreviate = true)

        @JvmField
        val PERCENT = UnitFormat(
            "percent",
            TextColor.color(0xa6dd58),
            abbreviate = true
        )

        @JvmField
        val RESEARCH_POINTS = UnitFormat(
            "research_points",
            TextColor.color(0x70da65),
            abbreviate = false
        )

        @JvmField
        val CELSIUS = UnitFormat(
            "celsius",
            TextColor.color(0xe27f41),
            abbreviate = true
        )

        @JvmField
        val MILLIBUCKETS = UnitFormat(
            "buckets",
            TextColor.color(0xe3835f2),
            abbreviate = true,
            prefix = MetricPrefix.MILLI
        )

        @JvmField
        val MILLIBUCKETS_PER_SECOND = UnitFormat(
            "buckets_per_second",
            TextColor.color(0xe3835f2),
            abbreviate = true,
            prefix = MetricPrefix.MILLI
        )

        @JvmField
        val DAYS = UnitFormat(
            "days",
            TextColor.color(0xc9c786),
            abbreviate = true
        )

        @JvmField
        val HOURS = UnitFormat(
            "hours",
            TextColor.color(0xc9c786),
            abbreviate = true
        )

        @JvmField
        val MINUTES = UnitFormat(
            "minutes",
            TextColor.color(0xc9c786),
            abbreviate = true
        )

        @JvmField
        val SECONDS = UnitFormat(
            "seconds",
            TextColor.color(0xc9c786),
            abbreviate = true,
        )

        @JvmField
        val EXPERIENCE = UnitFormat(
            "experience",
            TextColor.color(0xb2e01a),
            abbreviate = true
        )

        @JvmField
        val EXPERIENCE_PER_SECOND = UnitFormat(
            "experience_per_second",
            TextColor.color(0xb2e01a),
            abbreviate = true
        )

        @JvmField
        val ITEMS = UnitFormat(
            "items",
            TextColor.color(0x09e2c2),
            abbreviate = false
        )

        @JvmField
        val ITEMS_PER_SECOND = UnitFormat(
            "items_per_second",
            TextColor.color(0x09e2c2),
            abbreviate = true,
            prefix = MetricPrefix.NONE
        )

        @JvmField
        val STACKS = UnitFormat(
            "stacks",
            TextColor.color(0x44d2e2),
            abbreviate = false
        )

        @JvmField
        val CYCLES_PER_SECOND = UnitFormat(
            "cycles_per_second",
            TextColor.color(0xb672bf),
            abbreviate = true
        )

        @JvmField
        val JOULES = UnitFormat(
            "joules",
            TextColor.color(0xF2A900),
            abbreviate = true
        )

        @JvmField
        val WATTS = UnitFormat(
            "watts",
            TextColor.color(0xF2A900),
            abbreviate = true
        )

        @JvmField
        val VOLTS = UnitFormat(
            "volts",
            TextColor.color(0xF2A900),
            abbreviate = true
        )

        @JvmField
        val AMPRERES = UnitFormat(
            "amperes",
            TextColor.color(0xF2A900),
            abbreviate = true
        )

        /**
         * Helper function that automatically formats a duration into days:hours:minutes:seconds
         */
        @JvmStatic
        @JvmOverloads fun formatDuration(duration: Duration, abbreviate: Boolean = true, useMillis: Boolean = false): Component {
            var component = Component.text()
            var isEmpty = true

            val days = duration.toDaysPart()
            if (days > 0) {
                component = component.append(
                    DAYS.format(days)
                        .abbreviate(false)
                )
                isEmpty = false
            }
            val hours = duration.toHoursPart()
            if (hours > 0) {
                if (!isEmpty) {
                    component = component.append(Component.text(" "))
                }
                component = component.append(
                    HOURS.format(hours)
                        .abbreviate(abbreviate)
                )
                isEmpty = false
            }
            val minutes = duration.toMinutesPart()
            if (minutes > 0) {
                if (!isEmpty) {
                    component = component.append(Component.text(" "))
                }
                component = component.append(
                    MINUTES.format(minutes)
                        .abbreviate(abbreviate)
                )
                isEmpty = false
            }
            val seconds = duration.toSecondsPart()
            if (seconds > 0 || (!useMillis && isEmpty)) {
                if (!isEmpty) {
                    component = component.append(Component.text(" "))
                }
                component = component.append(
                    SECONDS.format(seconds)
                        .abbreviate(abbreviate)
                )
                isEmpty = false
            }
            if (useMillis) {
                val millis = duration.toMillisPart()
                if (millis > 0 || isEmpty) {
                    if (!isEmpty) {
                        component = component.append(Component.text(" "))
                    }
                    component = component.append(
                        SECONDS.format(millis / 1000.0)
                            .prefix(MetricPrefix.MILLI)
                            .abbreviate(abbreviate)
                    )
                    isEmpty = false
                }
            }
            return component.build()
        }
    }
}
