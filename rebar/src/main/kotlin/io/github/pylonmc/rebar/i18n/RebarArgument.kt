package io.github.pylonmc.rebar.i18n

import net.kyori.adventure.text.*

/**
 * A [TranslationArgument] only to be used when translating Rebar keys
 */
class RebarArgument private constructor(val name: String, val value: ComponentLike) :
    VirtualComponentRenderer<Unit>, TranslationArgumentLike {

    override fun apply(context: Unit): ComponentLike {
        return value
    }

    override fun asTranslationArgument(): TranslationArgument {
        return TranslationArgument.component(
            Component.virtual(
                Unit::class.java,
                this
            ).append(value) // Append the value for comparison purposes, it'll get thrown out anyway
        )
    }

    override fun fallbackString(): String {
        return "rebar:${name}"
    }

    companion object {
        @JvmStatic
        fun of(name: String, value: ComponentLike): RebarArgument {
            return RebarArgument(name, value)
        }

        @JvmStatic
        fun of(name: String, value: String): RebarArgument {
            return of(name, Component.text(value))
        }

        @JvmStatic
        fun of(name: String, value: Int): RebarArgument {
            return of(name, Component.text(value))
        }

        @JvmStatic
        fun of(name: String, value: Long): RebarArgument {
            return of(name, Component.text(value))
        }

        @JvmStatic
        fun of(name: String, value: Double): RebarArgument {
            return of(name, Component.text(value))
        }

        @JvmStatic
        fun of(name: String, value: Float): RebarArgument {
            return of(name, Component.text(value))
        }

        @JvmStatic
        fun of(name: String, value: Boolean): RebarArgument {
            return of(name, Component.text(value))
        }

        @JvmStatic
        fun of(name: String, value: Char): RebarArgument {
            return of(name, Component.text(value))
        }
    }
}