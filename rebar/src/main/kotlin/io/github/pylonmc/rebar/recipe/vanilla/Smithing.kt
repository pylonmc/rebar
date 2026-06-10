package io.github.pylonmc.rebar.recipe.vanilla

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.recipe.ingredient.FluidOrItem
import io.github.pylonmc.rebar.recipe.ingredient.FluidOrItemChoice
import io.github.pylonmc.rebar.recipe.ingredient.ItemChoice
import io.github.pylonmc.rebar.recipe.vanilla.DummyBukkitRebarRecipe.Companion.dummyKey
import io.github.pylonmc.rebar.util.gui.GuiItems
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.SmithingTrimRecipe
import org.bukkit.inventory.meta.trim.TrimPattern
import xyz.xenondevs.invui.gui.Gui

class DummySmithingRebarRecipe(
    val realRecipe: SmithingRebarRecipe,
    override val bukkitRecipe: SmithingRecipe
) : DummyBukkitRebarRecipe {
    override val inputs = emptyList<FluidOrItemChoice>()
    override val results = emptyList<FluidOrItem>()
    override fun display() = null
    override fun getKey() = bukkitRecipe.key
}

sealed class SmithingRebarRecipe(
    val result: FluidOrItem.Item,
    val copyDataComponents: Boolean,
    @JvmField val key: NamespacedKey,
) : BukkitRebarRecipe {
    abstract val template: ItemChoice?
    abstract val base: ItemChoice?
    abstract val addition: ItemChoice?

    override val inputs = listOfNotNull(template, base, addition)
    override val results = listOf(result)

    fun matches(templateInput: ItemStack?, baseInput: ItemStack?, addition: ItemStack?): Boolean {
        return ItemChoice.matches(templateInput, template)
            && ItemChoice.matches(baseInput, base)
            && ItemChoice.matches(addition, this.addition)
    }

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
    override val template: ItemChoice,
    override val base: ItemChoice,
    override val addition: ItemChoice,
    result: FluidOrItem.Item,
    copyDataComponents: Boolean,
    key: NamespacedKey,
    override val bukkitRecipe: SmithingTransformRecipe = SmithingTransformRecipe(key, result.item, template.toRepresentativeRecipeChoice(), base.toRepresentativeRecipeChoice(), addition.toRepresentativeRecipeChoice(), copyDataComponents)
) : SmithingRebarRecipe(result, copyDataComponents, key) {
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
    override val template: ItemChoice,
    override val base: ItemChoice,
    override val addition: ItemChoice,
    val trimPattern: TrimPattern,
    key: NamespacedKey,
    override val bukkitRecipe: SmithingTrimRecipe = SmithingTrimRecipe(key, template.toRepresentativeRecipeChoice(), base.toRepresentativeRecipeChoice(), addition.toRepresentativeRecipeChoice(), trimPattern)
) : SmithingRebarRecipe(FluidOrItem.Item.EMPTY, true, key) {
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

object DummySmithingRecipeType : DummyRecipeType<DummySmithingRebarRecipe>(rebarKey("dummy_smithing"))

/**
 * Key: `minecraft:smithing_transform`
 */
object SmithingTransformRecipeType : VanillaRecipeType<SmithingTransformRebarRecipe, DummySmithingRebarRecipe>("smithing_transform", DummySmithingRecipeType) {
    override fun loadRecipe(key: NamespacedKey, section: ConfigSection): SmithingTransformRebarRecipe {
        val template = section.getOrThrow("template", ConfigAdapter.ITEM_CHOICE)
        val base = section.getOrThrow("base", ConfigAdapter.ITEM_CHOICE)
        val addition = section.getOrThrow("addition", ConfigAdapter.ITEM_CHOICE)
        val result = FluidOrItem.of(section.getOrThrow("result", ConfigAdapter.ITEM_STACK))
        val copyData = section.get("copydata", ConfigAdapter.BOOLEAN, true)
        return SmithingTransformRebarRecipe(template, base, addition, result, copyData, key)
    }

    override fun createDummyRecipeFor(recipe: SmithingTransformRebarRecipe): DummySmithingRebarRecipe {
        return DummySmithingRebarRecipe(
            recipe, SmithingTransformRecipe(
                dummyKey(recipe.key), recipe.bukkitRecipe.result,
                recipe.template.toDummyRecipeChoice(),
                recipe.base.toDummyRecipeChoice(),
                recipe.addition.toDummyRecipeChoice(),
                recipe.copyDataComponents
            )
        )
    }
}

/**
 * Key: `minecraft:smithing_trim`
 */
object SmithingTrimRecipeType : VanillaRecipeType<SmithingTrimRebarRecipe, DummySmithingRebarRecipe>("smithing_trim", DummySmithingRecipeType) {
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

    override fun createDummyRecipeFor(recipe: SmithingTrimRebarRecipe): DummySmithingRebarRecipe {
        return DummySmithingRebarRecipe(
            recipe, SmithingTrimRecipe(
                dummyKey(recipe.key), recipe.template.toRepresentativeRecipeChoice(),
                recipe.base.toRepresentativeRecipeChoice(),
                recipe.addition.toRepresentativeRecipeChoice(),
                recipe.trimPattern
            )
        )
    }
}
