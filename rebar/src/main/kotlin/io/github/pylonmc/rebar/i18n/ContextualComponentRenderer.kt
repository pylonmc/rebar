package io.github.pylonmc.rebar.i18n

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.github.pylonmc.rebar.util.plainText
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgument
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import java.util.*

/**
 * Allows components to be dynamically chosen at translation time.
 * Some translations can only be resolved at translation-time; for example [UnitFormat] needs to know the locale
 * that is being translated to choose the correct plural form of a unit as per Unicode CLDR.
 *
 * Must be registered in [RebarRegistry.CONTEXTUAL_COMPONENT_RENDERERS] before use.
 */
abstract class ContextualComponentRenderer : Keyed {

    abstract fun render(locale: Locale, data: JsonElement): ComponentLike

    /**
     * Creates a component that uniquely identifies this renderer, and will be [render]ed with this renderer come translation time.
     *
     * @param data the data that will be supplied to [render]
     */
    fun createComponent(data: JsonElement): Component {
        return Component.translatable(
            MARKER_KEY,
            TranslationArgument.component(Component.text(key.toString())),
            TranslationArgument.component(Component.text(data.toString()))
        )
    }

    /**
     * The translator handling [ContextualComponentRenderer]s. Automatically registered.
     */
    object Translator : net.kyori.adventure.translation.Translator {

        override fun name() = rebarKey("language_dependent_translator")

        override fun translate(key: String, locale: Locale) = null

        override fun canTranslate(key: String, locale: Locale): Boolean {
            return key == MARKER_KEY
        }

        override fun translate(component: TranslatableComponent, locale: Locale): Component? {
            if (component.key() != MARKER_KEY) return null
            val (rendererArg, dataArg) = component.arguments()
            val renderer = RebarRegistry.CONTEXTUAL_COMPONENT_RENDERERS.getOrThrow(NamespacedKey.fromString(rendererArg.asComponent().plainText)!!)
            val data = JsonParser.parseString(dataArg.asComponent().plainText)
            return renderer.render(locale, data).asComponent()
        }
    }

    companion object {

        private const val MARKER_KEY = "rebar.never gonna give you up never gonna let you down.https://www.youtube.com/watch?v=dQw4w9WgXcQ"

        init {
            GlobalTranslator.translator().addSource(Translator)
        }
    }
}