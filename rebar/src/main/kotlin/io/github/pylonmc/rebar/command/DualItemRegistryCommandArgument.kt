package io.github.pylonmc.rebar.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import java.util.concurrent.CompletableFuture

object DualItemRegistryCommandArgument : CustomArgumentType.Converted<ItemTypeWrapper, NamespacedKey> {
    val ERROR_UNKNOWN = DynamicCommandExceptionType {
        MessageComponentSerializer.message().serialize(Component.text("Unknown item key: $it"))
    }

    override fun convert(nativeType: NamespacedKey): ItemTypeWrapper {
        try {
            return ItemTypeWrapper.invoke(nativeType)
        } catch (_: IllegalArgumentException) {
            // if no namespace is provided in the command execution, it uses the minecraft namespace
            if (nativeType.namespace == "minecraft") {
                var found: RebarItemSchema? = null
                for (schema in RebarRegistry.ITEMS) {
                    if (schema.key.key == nativeType.key) {
                        if (found != null) {
                            throw ERROR_UNKNOWN.create(nativeType)
                        }
                        found = schema
                    }
                }

                if (found != null) {
                    return ItemTypeWrapper.Rebar(found)
                }
            }
            throw ERROR_UNKNOWN.create(nativeType)
        }
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val input = builder.remainingLowerCase
        for (key in Registry.ITEM.keyStream()) {
            val key = key.toString()
            if (input in key) {
                builder.suggest(key)
            }
        }
        for (key in RebarRegistry.ITEMS.getKeys()) {
            val key = key.toString()
            if (input in key) {
                builder.suggest(key)
            }
        }
        return builder.buildFuture()
    }

    override fun getNativeType() = ArgumentTypes.namespacedKey()
}