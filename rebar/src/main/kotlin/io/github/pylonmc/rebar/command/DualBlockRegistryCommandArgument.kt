package io.github.pylonmc.rebar.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import java.util.concurrent.CompletableFuture

object DualBlockRegistryCommandArgument : CustomArgumentType<NamespacedKey, NamespacedKey> {
    val ERROR_UNKNOWN = DynamicCommandExceptionType {
        MessageComponentSerializer.message().serialize(Component.text("Unknown block key: $it"))
    }

    override fun parse(reader: StringReader): NamespacedKey = ArgumentTypes.namespacedKey().parse(reader)

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val input = builder.remainingLowerCase
        for (key in Registry.BLOCK.keyStream()) {
            val key = key.toString()
            if (input in key) {
                builder.suggest(key)
            }
        }
        for (key in RebarRegistry.BLOCKS.getKeys()) {
            val key = key.toString()
            if (input in key) {
                builder.suggest(key)
            }
        }
        return builder.buildFuture()
    }

    override fun getNativeType() = ArgumentTypes.namespacedKey()
}