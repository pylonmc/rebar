package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.interfaces.RecipeProcessorRebarBlock
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object RecipeProcessorDataPersistentDataType : PersistentDataType<PersistentDataContainer, RecipeProcessorRebarBlock.RecipeProcessorData<*>> {

    private val RECIPE_TYPE_KEY = rebarKey("recipe_type", RebarSerializers.KEYED.keyedTypeFrom { key -> RebarRegistry.RECIPE_TYPES.getOrThrow(key) })
    private val CURRENT_RECIPE_KEY = rebarKey("current_recipe", RebarSerializers.NAMESPACED_KEY)
    private val RECIPE_TIME_TICKS_KEY = rebarKey("recipe_time_ticks", RebarSerializers.INTEGER)
    private val RECIPE_TICKS_REMAINING_KEY = rebarKey("recipe_ticks_remaining", RebarSerializers.INTEGER)
    private val PROGRESS_ITEM_KEY = rebarKey("progress_item", RebarSerializers.PROGRESS_ITEM)
    private val LAST_RECIPE_KEY = rebarKey("last_recipe", RebarSerializers.NAMESPACED_KEY)

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<RecipeProcessorRebarBlock.RecipeProcessorData<*>> = RecipeProcessorRebarBlock.RecipeProcessorData::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): RecipeProcessorRebarBlock.RecipeProcessorData<*> {
        val recipeType = primitive.get(RECIPE_TYPE_KEY)!!
        return RecipeProcessorRebarBlock.RecipeProcessorData(
            recipeType,
            primitive.get(CURRENT_RECIPE_KEY)?.let{ recipeType.getRecipeOrThrow(it) },
            primitive.get(RECIPE_TIME_TICKS_KEY),
            primitive.get(RECIPE_TICKS_REMAINING_KEY),
            primitive.get(PROGRESS_ITEM_KEY),
            primitive.get(LAST_RECIPE_KEY)?.let{ recipeType.getRecipeOrThrow(it) },
        )
    }

    override fun toPrimitive(complex: RecipeProcessorRebarBlock.RecipeProcessorData<*>, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        pdc.setNullable(RECIPE_TYPE_KEY, complex.recipeType)
        pdc.setNullable(CURRENT_RECIPE_KEY, complex.currentRecipe?.key)
        pdc.setNullable(RECIPE_TIME_TICKS_KEY, complex.recipeTimeTicks)
        pdc.setNullable(RECIPE_TICKS_REMAINING_KEY, complex.recipeTicksRemaining)
        pdc.setNullable(PROGRESS_ITEM_KEY, complex.progressItem)
        pdc.setNullable(LAST_RECIPE_KEY, complex.lastRecipe?.key)
        return pdc
    }
}
