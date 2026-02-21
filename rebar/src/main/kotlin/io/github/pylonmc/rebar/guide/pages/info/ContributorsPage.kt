package io.github.pylonmc.rebar.guide.pages.info

import io.github.pylonmc.rebar.guide.button.AddonPageButton
import io.github.pylonmc.rebar.guide.pages.base.SimpleDynamicGuidePage
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.getContributors
import io.github.pylonmc.rebar.util.rebarKey

object ContributorsPage : SimpleDynamicGuidePage(
    rebarKey("contributors"),
    {
        RebarRegistry.ADDONS.getValues().filter { addon ->
            getContributors(addon).isNotEmpty()
        }.map { addon ->
            AddonPageButton(addon, AddonContributorsPage(addon))
        }
    }
)