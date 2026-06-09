package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.recipe.ItemChoice
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection

object ItemChoiceConfigAdapter : ConfigAdapter<ItemChoice> {

    override val type = ItemChoice::class.java

    private fun fromString(string: String): ItemChoice.Builder {
        val builder = ItemChoice.Builder()
        if (string.startsWith("#")) {
            // e.g. '#minecraft:acacia_logs'
            val tag = ConfigAdapter.ITEM_TAG.convert(string)
            for (value in tag.values) {
                builder.addFuzzy(value)
            }
        } else if (string.contains("[")) {
            // e.g. 'minecraft:potion[potion_contents={potion:"healing"}]'
            val stack = ConfigAdapter.ITEM_STACK.convert(string)
            builder.addFuzzy(stack)
        } else {
            // e.g. 'minecraft:apple'
            // e.g. 'pylon:fluid_pipe_copper'
            val type = ConfigAdapter.ITEM_TYPE_WRAPPER.convert(string)
            builder.addFuzzy(type)
        }
        return builder
    }

    private fun fromConfigSection(map: Map<String, Any?>): ItemChoice.Builder {
        val item = ConfigAdapter.ITEM_TYPE_WRAPPER.convert(map["item"] ?: error("You must specify an item for recipe inputs"))
        val exact = map["exact"]?.let { ConfigAdapter.BOOLEAN.convert(it) } ?: false
        val ignore = map["ignore"]?.let {
            ConfigAdapter.SET.from(ConfigAdapter.KEYED.fromRegistry(Registry.DATA_COMPONENT_TYPE)).convert(it)
        } ?: emptySet()

        check(ignore.isEmpty() || exact) {
            "You cannot have ignored components unless 'exact' is true"
        }

        val builder = ItemChoice.Builder()
        if (!exact) {
            builder.addFuzzy(item)
        } else {
            builder.addExact(item, ignore)
        }

        return builder
    }

    override fun convert(value: Any): ItemChoice = when (value) {
        is Pair<*, *> -> {
            // e.g. 'pylon:fluid_pipe_bronze: 5'
            fromString(value.first!! as String)
                .amount(ConfigAdapter.INTEGER.convert(value.second!!))
                .build()
        }

        is ConfigurationSection, is Map<*, *> -> {
            // e.g.
            // input:
            //   amount: 5
            //   choices:
            //   - item: pylon:bronze_pickaxe
            //     exact: true
            //     ignore:
            //     - max_damage
            //   - item: pylon:bronze_ingot
            //   - item: minecraft:stone-pickaxe

            val map = MapConfigAdapter.STRING_TO_ANY.convert(value)

            val amount = ConfigAdapter.INTEGER.convert(map["amount"] ?: error("You must specify a recipe amount e.g. 'amount: 5'"))
            val choices = ListConfigAdapter.from(ConfigAdapter.ANY).convert(map["choices"] ?: error("You must specify recipe choices e.g. 'choices: [pylon:fluid_pipe_copper, minecraft:apple]'"))

            val internalChoices = mutableListOf<ItemChoice.InternalItemChoice>()
            for (choice in choices) {
                val choiceBuilder = when (choice) {
                    is String -> fromString(choice)
                    else -> fromConfigSection(MapConfigAdapter.STRING_TO_ANY.convert(choice))
                }
                internalChoices.addAll(choiceBuilder.internalChoices)
            }
            ItemChoice(internalChoices, amount)
        }

        else -> throw IllegalArgumentException("Cannot convert $value to ItemChoice")
    }
}
