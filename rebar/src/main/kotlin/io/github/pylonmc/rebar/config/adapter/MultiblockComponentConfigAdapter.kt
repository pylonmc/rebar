package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.block.BlockTypeWrapper
import io.github.pylonmc.rebar.block.interfaces.SimpleRebarMultiblock.MultiblockComponent
import org.bukkit.configuration.ConfigurationSection

/**
 * A [ConfigAdapter] for [io.github.pylonmc.rebar.block.interfaces.SimpleRebarMultiblock.MultiblockComponent]s,
 * which match any of a set of vanilla or Rebar blocks.
 *
 * Each block can be a key, a block tag, or a vanilla block with block states.
 *
 * For example:
 * ```yaml
 * single-block: pylon:bronze_block
 * block-with-states: minecraft:oak_stairs[facing=north]
 * block-tag: '#minecraft:logs'
 * list: [minecraft:stone, pylon:bronze_block]
 * section:
 *   choices:
 *   - minecraft:stone
 *   - pylon:bronze_block
 *   - '#minecraft:logs'
 * ```
 */
object MultiblockComponentConfigAdapter : ConfigAdapter<MultiblockComponent> {

    override val type = MultiblockComponent::class.java

    private fun fromString(string: String): Collection<BlockTypeWrapper> = when {
        // e.g. '#minecraft:logs'
        string.startsWith("#") -> ConfigAdapter.BLOCK_TAG.convert(string).values
        // e.g. 'minecraft:oak_stairs[facing=north]'
        string.contains("[") -> listOf(BlockTypeWrapper(ConfigAdapter.BLOCK_DATA.convert(string)))
        // e.g. 'minecraft:stone'
        // e.g. 'pylon:bronze_block'
        else -> listOf(ConfigAdapter.BLOCK_TYPE_WRAPPER.convert(string))
    }

    private fun fromChoices(choices: List<String>): MultiblockComponent {
        val blocks = choices.flatMap(::fromString)
        return MultiblockComponent.of(
            blocks.filterIsInstance<BlockTypeWrapper.Vanilla>().map { it.blockData },
            blocks.filterIsInstance<BlockTypeWrapper.Rebar>().map { it.blockSchema.key }
        )
    }

    override fun convert(value: Any): MultiblockComponent = when (value) {
        is String -> fromChoices(listOf(value))

        // e.g. [minecraft:stone, pylon:bronze_block]
        is List<*> -> fromChoices(ConfigAdapter.LIST.from(ConfigAdapter.STRING).convert(value))

        is ConfigurationSection, is Map<*, *> -> {
            // e.g.
            // block:
            //   choices:
            //   - minecraft:stone
            //   - pylon:bronze_block
            //   - '#minecraft:logs'
            val map = MapConfigAdapter.STRING_TO_ANY.convert(value)
            fromChoices(
                ConfigAdapter.LIST.from(ConfigAdapter.STRING).convert(
                    map["choices"] ?: error("You must specify block choices e.g. 'choices: [minecraft:stone, pylon:bronze_block]'")
                )
            )
        }

        else -> throw IllegalArgumentException("Cannot convert $value to MultiblockComponent")
    }
}
