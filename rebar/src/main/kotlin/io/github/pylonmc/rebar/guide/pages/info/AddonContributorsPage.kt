package io.github.pylonmc.rebar.guide.pages.info

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.guide.button.ContributorButton
import io.github.pylonmc.rebar.guide.pages.base.SimpleDynamicGuidePage
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.util.getContributors
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.withArguments
import net.kyori.adventure.text.Component

class AddonContributorsPage(
    val addon: RebarAddon
) : SimpleDynamicGuidePage(
    rebarKey("contributors_addon"),
    {
        getContributors(addon).map { contributor ->
            ContributorButton(addon, contributor)
        }
    }
) {
    override val title: Component = super.title.withArguments(listOf(RebarArgument.of("addon", addon.displayName)))
}