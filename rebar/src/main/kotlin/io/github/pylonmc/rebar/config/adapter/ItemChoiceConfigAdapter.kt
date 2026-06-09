package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.ItemChoice
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection

object ItemChoiceConfigAdapter : ConfigAdapter<ItemChoice> {

    override val type = ItemChoice::class.java

    override fun convert(value: Any): ItemChoice = when (value) {
        is String -> convert(value to 1)

        is Pair<*, *> -> {
            val ingredientString = ConfigAdapter.STRING.convert(value.first!!)
            val amount = ConfigAdapter.INTEGER.convert(value.second!!)

            val builder = ItemChoice.Builder()
            if (ingredientString.startsWith("#")) {
                // e.g. '#minecraft:acacia_logs: 4'
                val tag = ConfigAdapter.ITEM_TAG.convert(value.first!!)
                for (value in tag.values) {
                    builder.addFuzzy(value, amount)
                }
            } else if (ingredientString.contains("[")) {
                // e.g. 'minecraft:potion[potion_contents={potion:"healing"}]'
                val stack = ConfigAdapter.ITEM_STACK.convert(value)
                builder.addFuzzy(stack, amount)
            } else {
                // e.g. 'minecraft:apple'
                // e.g. 'pylon:fluid_pipe_copper'
                val type = ConfigAdapter.ITEM_TYPE_WRAPPER.convert(ingredientString)
                builder.addFuzzy(type, amount)
            }

            builder.build()
        }

        is ConfigurationSection, is Map<*, *> -> {
            // e.g.
            // input:
            //   item: pylon:bronze_pickaxe
            //   amount: 5
            //   exact: true
            //   ignore:
            //     - max_damage

            val map = MapConfigAdapter.STRING_TO_ANY.convert(value)
            val item = ConfigAdapter.ITEM_TYPE_WRAPPER.convert(map["item"] ?: error("You must specify an item for recipe inputs"))
            val amount = map["amount"]?.let { ConfigAdapter.INTEGER.convert(it) } ?: 1
            val exact = map["exact"]?.let { ConfigAdapter.BOOLEAN.convert(it) } ?: false
            val ignore = map["ignore"]?.let {
                ConfigAdapter.SET.from(ConfigAdapter.KEYED.fromRegistry(Registry.DATA_COMPONENT_TYPE)).convert(it)
            } ?: emptySet()

            check(ignore.isEmpty() || exact) {
                "You cannot have ignored components unless 'exact' is true"
            }

            val builder = ItemChoice.Builder()
            if (!exact) {
                builder.addFuzzy(item, amount)
            } else {
                builder.addExact(item, amount, ignore)
            }

            builder.build()
        }

        is List<*> -> {
            // e.g.
            // input:
            //   - pylon:tin_ingot: 4
            //   - item: pylon:tin_ingot
            //     amount: 9
            //
            //   - pylon:steel_ingot: 7
            //   ...
            val finalChoice = ItemChoice.Builder().build()
            for (choice in value.map { convert(it!!) }) {
                finalChoice.internalChoices.addAll(choice.internalChoices)
            }
            finalChoice
        }
        else -> throw IllegalArgumentException("Cannot convert $value to ItemChoice")
    }
}
