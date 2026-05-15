package io.github.pylonmc.rebar.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.text.Component
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import java.util.concurrent.CompletableFuture

class RegistryCommandArgument<T : Keyed>(private val registry: RebarRegistry<T>) :
    CustomArgumentType.Converted<T, NamespacedKey> {

    @Suppress("PrivatePropertyName")
    private val ERROR_UNKNOWN = DynamicCommandExceptionType {
        MessageComponentSerializer.message().serialize(Component.text("Unknown key in ${registry.key}: $it"))
    }

    override fun convert(nativeType: NamespacedKey): T {
        return registry[nativeType] ?: throw ERROR_UNKNOWN.create(nativeType)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val input = builder.remainingLowerCase
        for (key in registry.getKeys()) {
            if (input in key.toString()) {
                builder.suggest(key.toString())
            }
        }
        return builder.buildFuture()
    }

    override fun getNativeType() = ArgumentTypes.namespacedKey()
}