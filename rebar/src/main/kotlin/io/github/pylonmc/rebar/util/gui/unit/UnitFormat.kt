@file:Suppress("unused")

package io.github.pylonmc.rebar.util.gui.unit

import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.text.PluralRules
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.i18n.LocaleDependentComponentRenderer
import io.github.pylonmc.rebar.i18n.RebarTranslator
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.translation.GlobalTranslator
import org.jetbrains.annotations.ApiStatus
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Duration
import java.util.*

/**
 * Handles formatting of a specific unit. Call [format] to format a value using this unit.
 *
 * @param name The base English name of the unit, preferably plural (ex "meters")
 * @param forms A map of [PluralForm]s to components for each plural form that represent this unit as per [CLDR](https://www.unicode.org/cldr/charts/42/supplemental/language_plural_rules.html).
 * @param abbreviation A component representing the abbreviated form of this unit (kg, m, L, etc). May be null to indicate that the unit does not have an abbreviation.
 * @param format The format string that is used as the base into which the value and unit are substituted. The string has 2 placeholders:
 * `v`, which is replaced with the value, and `u`, with is replaced with the unabbreviated unit name. For example, the default format string is `"v u"`.
 * `v` is replaced with the value (ex 3 to make `"3 u"`), and `u` is replaced with the unit (ex `"3 seggans"`)
 * @param abbrFormat Same as [format] but `u` is replaced with the abbreviated unit instead. Separate from [format] in order to allow things
 * like dollars (`$3`, using format string `"uv"`) or percent (`3%`, using format string `"vu"`). Default is `"v u"``.
 * @param defaultPrefix The prefix (kilo, nano, etc) used for this unit unless specified while formatting.
 * For example, if you create a 'grams' unit and specify [MetricPrefix.KILO] as the default prefix, calling
 * [format] with 100 will return '100 kilograms'
 * @param defaultStyle The style to apply to the unit (not the value) to the output.
 */

class UnitFormat @JvmOverloads constructor(
    val name: String,
    val forms: Map<PluralForm, Component>,
    val abbreviation: Component? = null,
    val format: String = "v u",
    val abbrFormat: String = "v u",
    val defaultPrefix: MetricPrefix = MetricPrefix.NONE,
    val defaultStyle: Style = Style.empty()
) {

    /**
     * @param addon The addon this unit is tied to.
     * @param name The name of this unit. Proper plural forms will be constructed as `<addon>.unit.<name>.<plural_keyword>` (see [PluralForm.keyword]).
     * The unit will have an abbreviation if the [addon]'s [RebarTranslator] can translate `<addon>.unit.<name>.abbr` in the addon's default language.
     * @param format The format string that is used as the base into which the value and unit are substituted. The string has 2 placeholders:
     * `v`, which is replaced with the value, and `u`, with is replaced with the unabbreviated unit name. For example, the default format string is `"v u"`.
     * `v` is replaced with the value (ex 3 to make `"3 u"`), and `u` is replaced with the unit (ex `"3 seggans"`)
     * @param abbrFormat Same as [format] but `u` is replaced with the abbreviated unit instead. Separate from [format] in order to allow things
     * like dollars (`$3`, using format string `"uv"`) or percent (`3%`, using format string `"vu"`). Default is `"v u"``.
     * @param defaultPrefix The prefix (kilo, nano, etc) used for this unit unless specified while formatting.
     * For example, if you create a 'grams' unit and specify [MetricPrefix.KILO] as the default prefix, calling
     * [format] with 100 will return '100 kilograms'
     * @param defaultStyle The style to apply to the unit (not the value) to the output.
     */
    @JvmOverloads
    constructor(
        addon: RebarAddon,
        name: String,
        format: String = "v u",
        abbrFormat: String = "v u",
        defaultPrefix: MetricPrefix = MetricPrefix.NONE,
        defaultStyle: Style = Style.empty()
    ) : this(
        name = name,
        forms = PluralForm.entries.associateWith { Component.translatable("${addon.key.namespace}.unit.$name.${it.keyword}") },
        abbreviation = Component.translatable("${addon.key.namespace}.unit.$name.abbr")
            .takeIf { addon.translator.canTranslate(it.key(), addon.defaultLanguage) },
        format = format,
        abbrFormat = abbrFormat,
        defaultPrefix = defaultPrefix,
        defaultStyle = defaultStyle
    )

    init {
        namedUnits[name] = this
    }

    /**
     * Disables the use of this unit in the custom `<unit:[name]>` tag in [Rebar's custom MiniMessage parser][io.github.pylonmc.rebar.i18n.customMiniMessage]
     *
     * @return this [UnitFormat]
     */
    fun disallowUseInUnitTag() = apply { namedUnits.remove(name) }

    /**
     * Returns a **new** [UnitFormat] with the same parameters as this one but with a [name]
     */
    fun withName(name: String) = copy(name = name)

    /**
     * Returns a **new** [UnitFormat] with the same parameters as this one but with different [forms]
     */
    fun withForms(forms: Map<PluralForm, Component>) = copy(forms = forms)

    /**
     * Returns a **new** [UnitFormat] with the same parameters as this one but with a different [abbreviation]
     */
    fun withAbbreviation(abbreviation: Component?) = copy(abbreviation = abbreviation)

    /**
     * Returns a **new** [UnitFormat] with the same parameters as this one but with a different [format]
     */
    fun withFormat(format: String) = copy(format = format)

    /**
     * Returns a **new** [UnitFormat] with the same parameters as this one but with a different [abbrFormat]
     */
    fun withAbbrFormat(abbrFormat: String) = copy(abbrFormat = abbrFormat)

    /**
     * Returns a **new** [UnitFormat] with the same parameters as this one but with a different [defaultPrefix]
     */
    fun withDefaultPrefix(prefix: MetricPrefix) = copy(defaultPrefix = prefix)

    /**
     * Returns a **new** [UnitFormat] with the same parameters as this one but with a different [defaultStyle]
     */
    fun withDefaultStyle(style: Style) = copy(defaultStyle = style)

    @JvmSynthetic
    fun copy(
        name: String = this.name,
        forms: Map<PluralForm, Component> = this.forms,
        abbreviation: Component? = this.abbreviation,
        format: String = this.format,
        abbrFormat: String = this.abbrFormat,
        defaultPrefix: MetricPrefix = this.defaultPrefix,
        defaultStyle: Style = this.defaultStyle
    ) = UnitFormat(name, forms, abbreviation, format, abbrFormat, defaultPrefix, defaultStyle)

    fun format(value: BigDecimal) = Formatted(FormattedValue.Number(value.stripTrailingZeros()))

    fun format(value: Int) = format(value.toLong())

    fun format(value: Long) = format(value.toBigDecimal())

    fun format(value: Float) = format(value.toDouble())

    fun format(value: Double): Formatted {
        return Formatted(
            when {
                value.isInfinite() && value > 0 -> FormattedValue.Infinity
                value.isInfinite() && value < 0 -> FormattedValue.NegativeInfinity
                value.isNaN() -> FormattedValue.NaN
                else -> FormattedValue.Number(value.toBigDecimal())
            }
        )
    }

    @ApiStatus.Internal
    internal sealed interface FormattedValue {
        data class Number(val value: BigDecimal) : FormattedValue
        data object NaN : FormattedValue
        data object Infinity : FormattedValue
        data object NegativeInfinity : FormattedValue
    }

    /**
     * Represents a value that has already been formatted.
     * You can use this class to override how an already-formatted value is displayed.
     */
    inner class Formatted @ApiStatus.Internal internal constructor(private val value: FormattedValue) : ComponentLike {
        private var sigFigs: Int? = null
        private var decimalPlaces: Int? = null
        private var forceDecimalPlaces = false
        private var abbreviate = true
        private var unitStyle = defaultStyle
        private var valueStyle = Style.empty()
        private var prefix: MetricPrefix? = defaultPrefix
        private val badPrefixes = EnumSet.noneOf(MetricPrefix::class.java)

        /**
         * Sets the number of significant figures. Uses [RoundingMode.HALF_UP] for rounding.
         * For example, if this is set to `3`, then a value of `3.755` will be shown as `3.76`.
         */
        fun significantFigures(sigFigs: Int) = apply { this.sigFigs = sigFigs }

        /**
         * Sets the number of decimal places. This overrides significant figures if both are set.
         * If [force] is true, the formatted number will always have this many decimal places.
         * Uses [RoundingMode.HALF_UP] for rounding.
         */
        fun decimalPlaces(decimalPlaces: Int, force: Boolean) = apply {
            this.decimalPlaces = decimalPlaces
            this.forceDecimalPlaces = force
        }

        /**
         * Sets the number of decimal places. This overrides significant figures if both are set.
         * Uses [RoundingMode.HALF_UP] for rounding.
         */
        fun decimalPlaces(decimalPlaces: Int) = decimalPlaces(decimalPlaces, false)

        /**
         * Sets whether the abbreviation should be used instead of the full name.
         */
        fun abbreviate(abbreviate: Boolean) = apply { this.abbreviate = abbreviate }

        /**
         * Overrides the style of the unit.
         */
        fun unitStyle(style: Style) = apply { this.unitStyle = style }

        /**
         * Overrides the style of the value.
         */
        fun valueStyle(style: Style) = apply { this.valueStyle = style }

        /**
         * Overrides the default prefix. **This will not rescale the number like [selectPrefixAndRescale] does.**
         */
        fun prefix(prefix: MetricPrefix) = apply { this.prefix = prefix }

        /**
         * [selectPrefixAndRescale] will not use any prefixes in this collection when automatically selecting a prefix.
         */
        fun ignorePrefixes(prefixes: Collection<MetricPrefix>) = apply { badPrefixes.addAll(prefixes) }

        /**
         * [selectPrefixAndRescale] will not use any of these [prefixes] when automatically selecting a prefix.
         */
        fun ignorePrefixes(vararg prefixes: MetricPrefix) = apply { badPrefixes.addAll(prefixes) }

        /**
         * Automatically selects an appropriate prefix based on the value and rescales the value accordingly.
         * **Default prefix is ignored when using this method.**
         *
         * @param ignoreCommonlyUnusedPrefixes if true, ignores the prefixes in [MetricPrefix.COMMONLY_UNUSED_PREFIXES]
         */
        fun selectPrefixAndRescale(ignoreCommonlyUnusedPrefixes: Boolean) = apply {
            prefix = null
            if (ignoreCommonlyUnusedPrefixes) ignorePrefixes(MetricPrefix.COMMONLY_UNUSED_PREFIXES)
        }

        /**
         * Automatically selects an appropriate prefix based on the value and rescales the value accordingly.
         * **Default prefix is ignored when using this method.**
         * Ignores the prefixes in [MetricPrefix.COMMONLY_UNUSED_PREFIXES].
         */
        fun selectPrefixAndRescale() = selectPrefixAndRescale(true)

        /**
         * Builds a component representing the value and unit.
         */
        fun build() = LocaleDependentComponentRenderer { lang ->
            val number: Component
            val prefix: MetricPrefix
            val plural: PluralForm
            when (value) {
                is FormattedValue.Number -> {
                    val value = value.value
                    var usedValue = value.round(MathContext(sigFigs ?: value.precision(), RoundingMode.HALF_UP))
                    usedValue = usedValue.setScale(decimalPlaces ?: value.scale(), RoundingMode.HALF_UP)
                    if (!forceDecimalPlaces) {
                        usedValue = usedValue.stripTrailingZeros()
                    }

                    prefix = if (this.prefix == null) {
                        val exponent = value.precision() - value.scale() - if (value.signum() == 0) 0 else 1
                        val prefix = MetricPrefix.entries.firstOrNull { it.scale <= exponent && it !in badPrefixes }
                            ?: defaultPrefix
                        usedValue = usedValue.movePointLeft(prefix.scale)
                        prefix
                    } else {
                        this.prefix!!
                    }

                    val formatted = NumberFormatter.withLocale(lang).format(usedValue)
                    number = Component.text(formatted.toString())

                    val keyword = PluralRules.forLocale(lang).select(formatted)
                    plural = PluralForm.entries.first { it.keyword == keyword }
                }

                FormattedValue.Infinity -> {
                    number = Component.text("Infinity")
                    prefix = MetricPrefix.NONE
                    plural = PluralForm.OTHER
                }

                FormattedValue.NegativeInfinity -> {
                    number = Component.text("-Infinity")
                    prefix = MetricPrefix.NONE
                    plural = PluralForm.OTHER
                }

                FormattedValue.NaN -> {
                    number = Component.text("NaN")
                    prefix = MetricPrefix.NONE
                    plural = PluralForm.OTHER
                }
            }

            val unit = if (abbreviate && abbreviation != null) {
                Component.empty().style(unitStyle)
                    .append(prefix.abbreviationKey)
                    .append(abbreviation)
            } else {
                Component.empty().style(unitStyle)
                    .append(prefix.translationKey)
                    .append(forms[plural] ?: error("Missing plural form ${plural.keyword} for $lang"))
            }

            val configU = TextReplacementConfig.builder()
                .matchLiteral("u")
                .replacement(unit)
                .build()
            val configV = TextReplacementConfig.builder()
                .matchLiteral("v")
                .replacement(number.style(valueStyle))
                .build()

            val final = Component.text(if (abbreviate && abbreviation != null) abbrFormat else format)
                .replaceText(configU)
                .replaceText(configV)

            GlobalTranslator.render(final, lang)
        }.asComponent()

        /**
         * Alias for [build]
         */
        override fun asComponent() = build()
    }

    /**
     * Plural forms as found in the [Unicode CLDR](https://www.unicode.org/cldr/charts/42/supplemental/language_plural_rules.html)
     */
    enum class PluralForm(
        /**
         * The CLDR keyword for this plural form
         */
        val keyword: String
    ) {
        /**
         * CLDR keyword: `zero`
         */
        ZERO(PluralRules.KEYWORD_ZERO),

        /**
         * CLDR keyword: `one`
         */
        ONE(PluralRules.KEYWORD_ONE),

        /**
         * CLDR keyword: `two`
         */
        TWO(PluralRules.KEYWORD_TWO),

        /**
         * CLDR keyword: `few`
         */
        FEW(PluralRules.KEYWORD_FEW),

        /**
         * CLDR keyword: `many`
         */
        MANY(PluralRules.KEYWORD_MANY),

        /**
         * CLDR keyword: `other`
         */
        OTHER(PluralRules.KEYWORD_OTHER),
    }

    companion object {

        @JvmSynthetic
        internal val namedUnits = mutableMapOf<String, UnitFormat>()

        @JvmField
        val BLOCKS = UnitFormat(
            Rebar,
            "blocks",
            defaultStyle = Style.style(TextColor.color(0x1eaa56))
        )

        @JvmField
        val BLOCKS_PER_SECOND = UnitFormat(
            Rebar,
            "blocks_per_second",
            defaultStyle = Style.style(TextColor.color(0x1eaa56))
        )

        @JvmField
        val CHUNKS = UnitFormat(
            Rebar,
            "chunks",
            defaultStyle = Style.style(TextColor.color(0x136D37))
        )

        @JvmField
        val HEARTS = UnitFormat(
            Rebar,
            "hearts",
            defaultStyle = Style.style(TextColor.color(0xdb3b43))
        )

        @JvmField
        val PERCENT = UnitFormat(
            Rebar,
            "percent",
            defaultStyle = Style.empty(),
            abbrFormat = "vu"
        )

        @JvmField
        val RESEARCH_POINTS = UnitFormat(
            Rebar,
            "research_points",
            defaultStyle = Style.style(TextColor.color(0x70da65))
        )

        @JvmField
        val CELSIUS = UnitFormat(
            Rebar,
            "celsius",
            defaultStyle = Style.style(TextColor.color(0xe27f41))
        )

        @JvmField
        val MILLIBUCKETS = UnitFormat(
            Rebar,
            "buckets",
            defaultStyle = Style.style(TextColor.color(0xe3835f2)),
            defaultPrefix = MetricPrefix.MILLI
        )

        @JvmField
        val MILLIBUCKETS_PER_SECOND = UnitFormat(
            Rebar,
            "buckets_per_second",
            defaultStyle = Style.style(TextColor.color(0xe3835f2)),
            defaultPrefix = MetricPrefix.MILLI
        )

        @JvmField
        val MILLIBUCKETS_PER_ITEM = UnitFormat(
            Rebar,
            "buckets_per_item",
            defaultStyle = Style.style(TextColor.color(0xe3835f2)),
            defaultPrefix = MetricPrefix.MILLI
        )

        @JvmField
        val DAYS = UnitFormat(
            Rebar,
            "days",
            defaultStyle = Style.style(TextColor.color(0xc9c786))
        )

        @JvmField
        val HOURS = UnitFormat(
            Rebar,
            "hours",
            defaultStyle = Style.style(TextColor.color(0xc9c786))
        )

        @JvmField
        val MINUTES = UnitFormat(
            Rebar,
            "minutes",
            defaultStyle = Style.style(TextColor.color(0xc9c786))
        )

        @JvmField
        val SECONDS = UnitFormat(
            Rebar,
            "seconds",
            defaultStyle = Style.style(TextColor.color(0xc9c786)),
        )

        @JvmField
        val JOULES = UnitFormat(
            Rebar,
            "joules",
            defaultStyle = Style.style(TextColor.color(0xF2A900)),
            defaultPrefix = MetricPrefix.NONE
        )

        @JvmField
        val WATTS = UnitFormat(
            Rebar,
            "watts",
            defaultStyle = Style.style(TextColor.color(0xF2A900)),
            defaultPrefix = MetricPrefix.NONE
        )

        @JvmField
        val EXPERIENCE = UnitFormat(
            Rebar,
            "experience",
            defaultStyle = Style.style(TextColor.color(0xb2e01a))
        )

        @JvmField
        val EXPERIENCE_PER_SECOND = UnitFormat(
            Rebar,
            "experience_per_second",
            defaultStyle = Style.style(TextColor.color(0xb2e01a))
        )

        @JvmField
        val ITEMS = UnitFormat(
            Rebar,
            "items",
            defaultStyle = Style.style(TextColor.color(0x09e2c2))
        )

        @JvmField
        val ITEMS_PER_SECOND = UnitFormat(
            Rebar,
            "items_per_second",
            defaultStyle = Style.style(TextColor.color(0x09e2c2))
        )

        @JvmField
        val STACKS = UnitFormat(
            Rebar,
            "stacks",
            defaultStyle = Style.style(TextColor.color(0x44d2e2))
        )

        @JvmField
        val CYCLES = UnitFormat(
            Rebar,
            "cycles",
            defaultStyle = Style.style(TextColor.color(0xb672bf)),
            defaultPrefix = MetricPrefix.NONE
        )

        @JvmField
        val CYCLES_PER_SECOND = UnitFormat(
            Rebar,
            "cycles_per_second",
            defaultStyle = Style.style(TextColor.color(0xb672bf)),
            defaultPrefix = MetricPrefix.NONE
        )

        /**
         * Helper function that automatically formats a duration into `<days> <hours> <minutes> <seconds> <milliseconds>?`,
         * skipping any that are 0.
         *
         * @param duration the duration to format
         * @param abbreviate whether to abbreviate the units
         * @param useMillis whether to add milliseconds
         */
        @JvmStatic
        @JvmOverloads
        fun formatDuration(duration: Duration, abbreviate: Boolean = true, useMillis: Boolean = false): Component {
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
