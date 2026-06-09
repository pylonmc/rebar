package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.ItemChoice
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.SmithingTrimRecipe
import org.bukkit.inventory.meta.trim.TrimPattern
import xyz.xenondevs.invui.gui.Gui


sealed class SmithingRebarRecipe(
    val template: ItemChoice?,
    val base: ItemChoice?,
    val addition: ItemChoice?,
    val result: FluidOrItem.Item,
    val copyDataComponents: Boolean,
    @JvmField val key: NamespacedKey,
) : VanillaRebarRecipe {
    init {
        check(template != null || base != null || addition != null) {
            "There must be at least one non-null ingredient in a smithing recipe"
        }
    }

    override val inputs = listOfNotNull(template, base, addition)
    override val results = listOf(result)

    override fun display() = Gui.builder()
        .setStructure(
            "# # # # # # # # #",
            "# # # # # # # # #",
            "# b # 0 1 2 # r #",
            "# # # # # # # # #",
            "# # # # # # # # #",
        )
        .addIngredient('#', GuiItems.backgroundBlack())
        .addIngredient('b', ItemButton.of(ItemStack.of(Material.SMITHING_TABLE)))
        .addIngredient('0', ItemButton.of(template))
        .addIngredient('1', ItemButton.of(base))
        .addIngredient('2', ItemButton.of(addition))
        .addIngredient('r', ItemButton.of(result))
        .build()

    override fun getKey(): NamespacedKey = key
}

class SmithingTransformRebarRecipe(
    template: ItemChoice,
    base: ItemChoice,
    addition: ItemChoice,
    result: FluidOrItem.Item,
    copyDataComponents: Boolean,
    key: NamespacedKey,
    override val recipe: SmithingTransformRecipe = SmithingTransformRecipe(key, result.item, template.toRepresentativeRecipeChoice(), base.toRepresentativeRecipeChoice(), addition.toRepresentativeRecipeChoice(), copyDataComponents)
) : SmithingRebarRecipe(template, base, addition, result, copyDataComponents, key) {
    companion object {
        fun fromVanilla(recipe: SmithingTransformRecipe): SmithingTransformRebarRecipe {
            return SmithingTransformRebarRecipe(
                recipe.template.toItemChoice(),
                recipe.base.toItemChoice(),
                recipe.addition.toItemChoice(),
                FluidOrItem.of(recipe.result),
                recipe.willCopyDataComponents(),
                recipe.key,
                recipe
            )
        }
    }
}
class SmithingTrimRebarRecipe(
    template: ItemChoice,
    base: ItemChoice,
    addition: ItemChoice,
    val trimPattern: TrimPattern,
    key: NamespacedKey,
    override val recipe: SmithingTrimRecipe = SmithingTrimRecipe(key, template.toRepresentativeRecipeChoice(), base.toRepresentativeRecipeChoice(), addition.toRepresentativeRecipeChoice(), trimPattern)
) : SmithingRebarRecipe(template, base, addition, FluidOrItem.Item.EMPTY, true, key) {
    companion object {
        fun fromVanilla(recipe: SmithingTrimRecipe): SmithingTrimRebarRecipe {
            return SmithingTrimRebarRecipe(
                recipe.template.toItemChoice(),
                recipe.base.toItemChoice(),
                recipe.addition.toItemChoice(),
                recipe.trimPattern,
                recipe.key,
                recipe
            )
        }
    }
}

/**
 * Key: `minecraft:smithing_transform`
 */
object SmithingTransformRecipeType : VanillaRecipeType<SmithingTransformRebarRecipe>("smithing_transform") {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): SmithingTransformRebarRecipe {
        val template = section.getOrThrow("template", ConfigAdapter.ITEM_CHOICE)
        val base = section.getOrThrow("base", ConfigAdapter.ITEM_CHOICE)
        val addition = section.getOrThrow("addition", ConfigAdapter.ITEM_CHOICE)
        val result = FluidOrItem.of(section.getOrThrow("result", ConfigAdapter.ITEM_STACK))
        val copyData = section.get("copydata", ConfigAdapter.BOOLEAN, true)
        return SmithingTransformRebarRecipe(template, base, addition, result, copyData, key)
    }
}

/**
 * Key: `minecraft:smithing_trim`
 */
object SmithingTrimRecipeType : VanillaRecipeType<SmithingTrimRebarRecipe>("smithing_trim") {
    private val TRIM_PATTERN_ADAPTER = ConfigAdapter.KEYED.fromRegistry(
        RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN)
    )

    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): SmithingTrimRebarRecipe {
        val template = section.getOrThrow("template", ConfigAdapter.ITEM_CHOICE)
        val base = section.getOrThrow("base", ConfigAdapter.ITEM_CHOICE)
        val addition = section.getOrThrow("addition", ConfigAdapter.ITEM_CHOICE)
        val pattern = section.getOrThrow("pattern", TRIM_PATTERN_ADAPTER)
        return SmithingTrimRebarRecipe(template, base, addition, pattern, key)
    }
}
