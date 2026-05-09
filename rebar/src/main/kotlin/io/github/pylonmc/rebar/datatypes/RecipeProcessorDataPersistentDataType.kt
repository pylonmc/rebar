package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.block.base.RebarRecipeProcessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

internal object RecipeProcessorDataPersistentDataType : PersistentDataType<PersistentDataContainer, RebarRecipeProcessor.RecipeProcessorData<*>> {

    private val RECIPE_TYPE_KEY = rebarKey("recipe_type")
    private val CURRENT_RECIPE_KEY = rebarKey("current_recipe")
    private val RECIPE_TIME_TICKS_KEY = rebarKey("recipe_time_ticks")
    private val RECIPE_TICKS_REMAINING_KEY = rebarKey("recipe_ticks_remaining")
    private val PROGRESS_ITEM_KEY = rebarKey("progress_item")
    private val LAST_RECIPE_KEY = rebarKey("last_recipe")

    private val RECIPE_TYPE_TYPE = RebarSerializers.KEYED.keyedTypeFrom { key -> RebarRegistry.RECIPE_TYPES.getOrThrow(key) }

    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java

    override fun getComplexType(): Class<RebarRecipeProcessor.RecipeProcessorData<*>> = RebarRecipeProcessor.RecipeProcessorData::class.java

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): RebarRecipeProcessor.RecipeProcessorData<*> {
        val recipeType = primitive.get(RECIPE_TYPE_KEY, RECIPE_TYPE_TYPE)!!
        val recipePDT = RebarSerializers.KEYED.keyedTypeFrom { recipeType.getRecipeOrThrow(it) }
        return RebarRecipeProcessor.RecipeProcessorData(
            recipeType,
            primitive.get(CURRENT_RECIPE_KEY, recipePDT),
            primitive.get(RECIPE_TIME_TICKS_KEY, RebarSerializers.INTEGER),
            primitive.get(RECIPE_TICKS_REMAINING_KEY, RebarSerializers.INTEGER),
            primitive.get(PROGRESS_ITEM_KEY, RebarSerializers.PROGRESS_ITEM),
            primitive.get(LAST_RECIPE_KEY, recipePDT),
        )
    }

    override fun toPrimitive(complex: RebarRecipeProcessor.RecipeProcessorData<*>, context: PersistentDataAdapterContext): PersistentDataContainer {
        val pdc = context.newPersistentDataContainer()
        val recipePDT = RebarSerializers.KEYED.keyedTypeFrom { complex.recipeType!!.getRecipeOrThrow(it) }
        pdc.setNullable(RECIPE_TYPE_KEY, RECIPE_TYPE_TYPE, complex.recipeType)
        pdc.setNullable(CURRENT_RECIPE_KEY, recipePDT, complex.currentRecipe)
        pdc.setNullable(RECIPE_TIME_TICKS_KEY, RebarSerializers.INTEGER, complex.recipeTimeTicks)
        pdc.setNullable(RECIPE_TICKS_REMAINING_KEY, RebarSerializers.INTEGER, complex.recipeTicksRemaining)
        pdc.setNullable(PROGRESS_ITEM_KEY, RebarSerializers.PROGRESS_ITEM, complex.progressItem)
        pdc.setNullable(LAST_RECIPE_KEY, recipePDT, complex.lastRecipe)
        return pdc
    }
}
