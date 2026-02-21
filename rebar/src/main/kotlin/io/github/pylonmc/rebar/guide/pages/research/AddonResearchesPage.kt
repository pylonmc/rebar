package io.github.pylonmc.rebar.guide.pages.research

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.button.ResearchButton
import io.github.pylonmc.rebar.guide.pages.base.SimpleDynamicGuidePage
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style

/**
 * Shows all the researches for the given [addon].
 */
class AddonResearchesPage(val addon: RebarAddon) : SimpleDynamicGuidePage(
    rebarKey("researches_" + addon.key.key),
    {
        RebarRegistry.RESEARCHES.filter {
            it.key.namespace == addon.key.namespace && it.key !in RebarGuide.hiddenResearches
        }.map {
            ResearchButton(it)
        }
    }
) {
    override val title = Component.translatable("rebar.guide.page.researches_addon",
        RebarArgument.of("addon", addon.displayName))
}