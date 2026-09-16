package io.github.pylonmc.rebar.i18n

import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.*
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import java.util.*

/**
 * Allows components to be dynamically chosen at translation time.
 * Some translations can only be resoled at translation-time; for example [UnitFormat] needs to know the locale
 * that is being translated to choose the correct plural form of a unit as per Unicode CLDR.
 */
@FunctionalInterface
fun interface LocaleDependentComponentRenderer : VirtualComponentRenderer<Locale>, ComponentLike {

    override fun asComponent() = create(this)

    override fun fallbackString() = PlainTextComponentSerializer.plainText().serialize(apply(Locale.ENGLISH).asComponent())

    /**
     * The translator handling [LocaleDependentComponentRenderer]s. Automatically registered.
     */
    object Translator : net.kyori.adventure.translation.Translator {

        override fun name() = rebarKey("language_dependent_translator")

        override fun translate(key: String, locale: Locale) = null

        override fun canTranslate(key: String, locale: Locale): Boolean {
            return key == MARKER_KEY
        }

        override fun translate(component: TranslatableComponent, locale: Locale): Component? {
            if (component.key() != MARKER_KEY) return null
            val virtualComponent = component.arguments().firstOrNull()?.asComponent() as? VirtualComponent ?: return null
            val renderer = virtualComponent.renderer() as? LocaleDependentComponentRenderer ?: return null
            return renderer.apply(locale).asComponent()
        }
    }

    companion object {

        private const val MARKER_KEY = "rebar.never gonna give you up never gonna let you down.https://www.youtube.com/watch?v=dQw4w9WgXcQ"

        init {
            GlobalTranslator.translator().addSource(Translator)
        }

        /**
         * Creates a [Component] containing the correct markers that would allow the [Translator] to later correctly
         * invoke [LocaleDependentComponentRenderer.apply]
         */
        @JvmStatic
        fun create(renderer: LocaleDependentComponentRenderer): Component =
            Component.translatable(MARKER_KEY, Component.virtual(Locale::class.java, renderer))
    }
}