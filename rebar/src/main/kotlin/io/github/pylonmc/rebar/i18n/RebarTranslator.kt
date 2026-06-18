package io.github.pylonmc.rebar.i18n

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.RebarRegisterEvent
import io.github.pylonmc.rebar.event.RebarUnregisterEvent
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translator
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.editData
import io.github.pylonmc.rebar.util.mergeResource
import io.github.pylonmc.rebar.util.persistentData
import io.github.pylonmc.rebar.util.plainText
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.withArguments
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.*
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.Translator
import org.apache.commons.lang3.LocaleUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLocaleChangeEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.text.MessageFormat
import java.util.*
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

/**
 * The [Translator] for a given [RebarAddon]. This translator handles the translation of
 * any keys in the form of `<addon>.<key>`, where `<addon>` is the namespace of the addon
 * and `<key>` is the path to the translation key within the language files for that addon.
 *
 * Use [translator] to obtain an instance.
 */
class RebarTranslator private constructor(private val addon: RebarAddon) : Translator {

    private val addonNamespace = addon.key.namespace

    private val translations: MutableMap<Locale, ConfigSection> = mutableMapOf()

    val languages: Set<Locale>
        get() = translations.keys

    private val translationCache = mutableMapOf<Pair<Locale, String>, Component>()

    init {
        // Copy builtin language files
        val jarFile = JarFile(File(addon.javaClass.protectionDomain.codeSource.location.toURI()))
        jarFile.stream()
            .filter { it.name.startsWith("lang/") && it.name.endsWith(".yml") }
            .map { it.name.removePrefix("lang/") }
            .forEach { file -> mergeResource(addon, "lang/$file", "lang/$addonNamespace/$file") }

        loadTranslations()
    }

    private fun loadTranslations() {
        val langsDir = Rebar.dataPath.resolve("lang").resolve(addonNamespace)
        if (langsDir.exists()) {
            langsDir.listDirectoryEntries("*.yml").forEach {
                val split = it.nameWithoutExtension.split('_', limit = 3)
                val locale = Locale.of(
                    split.first(),
                    split.getOrNull(1).orEmpty(),
                    split.getOrNull(2).orEmpty()
                )
                val config = ConfigSection.fromOrThrow(it)
                translations[locale] = config
            }
        }
    }

    fun reload() {
        translationCache.clear()
        translations.clear()
        loadTranslations()
        for (player in Bukkit.getOnlinePlayers()) {
            NmsAccessor.instance.resendInventory(player)
        }
    }

    override fun canTranslate(key: String, locale: Locale): Boolean {
        return getRawTranslation(key, locale) != null
    }

    override fun translate(component: TranslatableComponent, locale: Locale): Component? {
        var translation = getRawTranslation(component.key(), locale) ?: return null
        for (arg in component.arguments()) {
            var componentArg = arg.asComponent()
            if (componentArg is TextComponent && componentArg.content().startsWith("rebar:")) {
                // was a rebar argument that got serialized to vanilla
                val argName = componentArg.content().removePrefix("rebar:")
                val argValue = componentArg.children().firstOrNull() ?: Component.empty()
                componentArg = RebarArgument.of(argName, argValue).asComponent()
            }

            if (componentArg !is VirtualComponent) continue
            val renderer = componentArg.renderer()
            if (renderer !is RebarArgument) continue
            val replacer = TextReplacementConfig.builder()
                .match("%${renderer.name}%")
                .replacement(GlobalTranslator.render(renderer.value.asComponent(), locale))
                .build()
            translation = translation.replaceText(replacer)
        }
        translation = translation.append(component.children())
        return translation
            .children(translation.children().map { GlobalTranslator.render(it, locale) })
            .style(translation.style().merge(component.style(), Style.Merge.Strategy.IF_ABSENT_ON_TARGET))
    }

    private fun getRawTranslation(translationKey: String, locale: Locale): Component? {
        return translationCache.getOrPut(locale to translationKey) {
            val parts = translationKey.split('.', limit = 2)
            if (parts.size < 2) return null
            val (addon, key) = parts
            if (addon != addonNamespace) return null
            val translations = findTranslations(locale) ?: findTranslations(this.addon.defaultLanguage) ?: return null
            val translation = translations.get(key, ConfigAdapter.STRING) ?: return null
            customMiniMessage.deserialize(translation)
        }
    }

    private fun findTranslations(locale: Locale): ConfigSection? {
        val languageRange = languageRanges.getOrPut(locale) {
            val lookupList = LocaleUtils.localeLookupList(locale)
            lookupList.reversed()
                .mapIndexed { index, value ->
                    Locale.LanguageRange(value.toLanguageTag(), (index + 1.0) / lookupList.size)
                }
                .sortedByDescending { it.weight }
        }
        return Locale.lookup(languageRange, this.translations.keys)?.let(translations::get)
    }

    override fun name(): Key = addon.key
    override fun translate(key: String, locale: Locale): MessageFormat? = null

    companion object : Listener {
        private val languageRanges = WeakHashMap<Locale, List<Locale.LanguageRange>>()

        private val translators = mutableMapOf<NamespacedKey, RebarTranslator>()

        private val originalNameKey = rebarKey("translation_original_name")
        private val originalLoreKey = rebarKey("translation_original_lore")
        private val originalTypeKey = rebarKey("translation_original_type")

        private val loreType = RebarSerializers.LIST.listTypeFrom(RebarSerializers.COMPONENT)

        private val storyTextKey = rebarKey("story_text")

        @JvmStatic
        var Player.storyText: Boolean by persistentData(storyTextKey, RebarSerializers.BOOLEAN, true)

        @JvmStatic
        @get:JvmName("getTranslatorForAddon")
        val RebarAddon.translator: RebarTranslator
            get() = translators[this.key]
                ?: error("Addon ${this.key} does not have a translator; did you forget to call registerWithRebar()?")

        /**
         * Modifies the [ItemStack] to translate its name and lore into the locale of the specified [player].
         */
        @JvmStatic
        @JvmOverloads
        @JvmName("translateItem")
        @Suppress("UnstableApiUsage")
        fun ItemStack.translate(player: Player, arguments: List<RebarArgument> = emptyList()) {
            fun isRebarOrAddon(component: Component): Boolean {
                if (component is TranslatableComponent) {
                    for (addon in RebarRegistry.ADDONS) {
                        if (component.key().startsWith(addon.key.namespace)) {
                            return true
                        }
                    }
                }
                return component.children().any(::isRebarOrAddon)
            }

            val locale = player.locale()

            editData(DataComponentTypes.ITEM_NAME) {
                if (!isRebarOrAddon(it)) return@editData it

                editPersistentDataContainer { pdc -> pdc.set(originalNameKey, RebarSerializers.COMPONENT, it) }

                if (!persistentDataContainer.getOrDefault(
                        ItemStackBuilder.disableNameHacksKey,
                        RebarSerializers.BOOLEAN,
                        false
                    )
                ) {
                    if (type == Material.PLAYER_HEAD) {
                        editData(DataComponentTypes.PROFILE) { profile ->
                            // Need to remove the name from the profile because it overrides item name
                            ResolvableProfile.resolvableProfile()
                                .uuid(profile.uuid())
                                .addProperties(profile.properties())
                                .build()
                        }
                    } else if (type.isPotion) {
                        // Potions are wacky wrt names, so we lie to the client about the type and set the model data
                        val oldStack = clone()
                        @Suppress("DEPRECATION")
                        type = Material.CLAY_BALL
                        check(type == Material.CLAY_BALL) { "ItemStack.setType no longer works" }
                        copyDataFrom(oldStack) { true }
                        if (!oldStack.isDataOverridden(DataComponentTypes.ITEM_MODEL)) {
                            editData(DataComponentTypes.ITEM_MODEL) { oldStack.type.key }
                        }
                        editPersistentDataContainer { pdc -> pdc.set(originalTypeKey, RebarSerializers.MATERIAL, oldStack.type) }
                    }
                }

                val concatenatedArguments: MutableList<TranslationArgumentLike> = arguments.toMutableList()
                if (it is TranslatableComponent) {
                    concatenatedArguments.addAll(it.arguments())
                }

                val translated = GlobalTranslator.render(it.withArguments(concatenatedArguments), locale)
                val result = if (translated is TranslatableComponent && translated.fallback() != null) {
                    Component.text(translated.fallback()!!)
                } else {
                    translated
                }

                // apply white color if not set
                if (result.style().color() == null) result.color(NamedTextColor.WHITE) else result
            }
            editData(DataComponentTypes.LORE) { lore ->
                val originalLore = lore.lines().toMutableList()

                // Add story text to lore if this is a Rebar item with story text and player has story text enabled
                val rebarItemSchema = RebarItemSchema.fromStack(this)
                if (rebarItemSchema != null && player.storyText) {
                    val storyKey = "${rebarItemSchema.key.namespace}.item.${rebarItemSchema.key.key}.story"
                    if (translators[rebarItemSchema.addon.key]!!.canTranslate(storyKey, locale)) {
                        if (originalLore.isNotEmpty()) {
                            originalLore.add(Component.empty()) // newline
                        }
                        originalLore.add(Component.translatable(storyKey, null as String?))
                    }
                }

                val newLore = originalLore.flatMap { line ->
                    if (!isRebarOrAddon(line)) return@flatMap listOf(line)
                    val concatenatedArguments: MutableList<TranslationArgumentLike> = arguments.toMutableList()
                    if (line is TranslatableComponent) {
                        concatenatedArguments.addAll(line.arguments())
                    }
                    val translated = GlobalTranslator.render(line.withArguments(concatenatedArguments), locale)
                    if (translated.plainText.isBlank()) return@flatMap emptyList()
                    splitByNewlines(translated).flatMap {
                        wrapLine(it)
                    }
                }

                if (originalLore == newLore) {
                    return
                }

                editPersistentDataContainer { pdc -> pdc.set(originalLoreKey, loreType, originalLore) }
                check(newLore.size <= 256) {
                    "Lore for item had too many lines ($locale) (256 lines max but had ${newLore.size}): ${newLore.map { it.plainText }}\\n"
                }
                ItemLore.lore(newLore)
            }
        }

        /**
         * Reverts any changes made to the item by the [translate] method
         */
        @JvmStatic
        @JvmName("translateItem")
        @Suppress("UnstableApiUsage")
        fun ItemStack.untranslate() {
            val originalName = persistentDataContainer.get(originalNameKey, RebarSerializers.COMPONENT)
            val originalLore = persistentDataContainer.get(originalLoreKey, loreType)
            val originalType = persistentDataContainer.get(originalTypeKey, RebarSerializers.MATERIAL)
            if (originalName != null) {
                setData(DataComponentTypes.ITEM_NAME, originalName)
                editPersistentDataContainer { pdc -> pdc.remove(originalNameKey) }
            }
            if (originalLore != null) {
                setData(DataComponentTypes.LORE, ItemLore.lore(originalLore))
                editPersistentDataContainer { pdc -> pdc.remove(originalLoreKey) }
            }
            if (originalType != null) {
                val oldStack = clone()
                @Suppress("DEPRECATION")
                type = originalType
                check(type == originalType) { "ItemStack.setType no longer works" }
                copyDataFrom(oldStack) { true }
                editPersistentDataContainer { pdc -> pdc.remove(originalTypeKey) }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onAddonRegister(event: RebarRegisterEvent) {
            if (event.registry != RebarRegistry.ADDONS) return
            val addon = event.value as? RebarAddon ?: return
            val translator = RebarTranslator(addon)
            GlobalTranslator.translator().addSource(translator)
            translators[addon.key] = translator
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onAddonUnregister(event: RebarUnregisterEvent) {
            if (event.registry != RebarRegistry.ADDONS) return
            val addon = event.value as? RebarAddon ?: return
            translators.remove(addon.key)?.let(GlobalTranslator.translator()::removeSource)
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onPlayerJoin(event: PlayerJoinEvent) {
            val player = event.player
            NmsAccessor.instance.registerTranslationHandler(player, PlayerTranslationHandler(player))
            // Since the recipe book is initially sent before the event, and therefore before
            // we can register the translation handler, we need to resend it here so that it
            // gets translated properly.
            NmsAccessor.instance.resendRecipeBook(player)
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onPlayerQuit(event: PlayerQuitEvent) {
            NmsAccessor.instance.unregisterTranslationHandler(event.player)
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onPlayerChangeLanguage(event: PlayerLocaleChangeEvent) {
            if (!event.player.isOnline) return
            NmsAccessor.instance.resendInventory(event.player)
            NmsAccessor.instance.resendRecipeBook(event.player)
        }
    }
}

private val Material.isPotion: Boolean
    get() = this == Material.POTION || this == Material.SPLASH_POTION ||
            this == Material.LINGERING_POTION || this == Material.TIPPED_ARROW
