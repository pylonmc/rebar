@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.command

import com.destroystokyo.paper.profile.PlayerProfile
import com.github.shynixn.mccoroutine.bukkit.launch
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.context.CommandContext
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlockSchema
import io.github.pylonmc.rebar.content.debug.DebugWaxedWeatheredCutCopperStairs
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.entity.display.transform.Rotation
import io.github.pylonmc.rebar.gametest.GameTestConfig
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.item.research.Research.Companion.researchPoints
import io.github.pylonmc.rebar.item.research.addResearch
import io.github.pylonmc.rebar.item.research.hasResearch
import io.github.pylonmc.rebar.item.research.removeResearch
import io.github.pylonmc.rebar.metrics.RebarMetrics
import io.github.pylonmc.rebar.particles.ConfettiParticle
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.mergeGlobalConfig
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.vanillaDisplayName
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver
import io.papermc.paper.command.brigadier.argument.resolvers.RotationResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.math.FinePosition
import kotlinx.coroutines.future.await
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import kotlin.reflect.typeOf
import io.papermc.paper.math.BlockPosition as PaperBlockPosition

private val guide = buildCommand("guide") {
    permission("rebar.command.guide")
    executesWithPlayer { player ->
        RebarMetrics.onCommandRun("/rb guide")
        player.inventory.addItem(RebarGuide.STACK)
    }
    argument("players", ArgumentTypes.players()) {
        permission("rebar.command.guide.others")
        executes {
            RebarMetrics.onCommandRun("/rb guide")
            val players = getArgument<List<Player>>("players")
            for (player in players) {
                player.inventory.addItem(RebarGuide.STACK)
            }
            val singular = players.size == 1
            source.sender.sendVanillaFeedback(
                "give.success." + if (singular) "single" else "multiple",
                Component.text(1),
                RebarGuide.STACK.vanillaDisplayName(),
                if (singular) players[0].name() else Component.text(players.size)
            )
        }
    }
}

private val give = buildCommand("give") {
    argument("players", ArgumentTypes.players()) {
        argument("item", RegistryCommandArgument(RebarRegistry.ITEMS)) {
            // Why does Brigadier not support default values for arguments?
            // https://github.com/Mojang/brigadier/issues/110

            fun givePlayers(context: CommandContext<CommandSourceStack>, amount: Int) {
                val item = context.getArgument<RebarItemSchema>("item")
                val players = context.getArgument<List<Player>>("players")
                val singular = players.size == 1
                for (player in players) {
                    player.inventory.addItem(item.getItemStack().asQuantity(amount))
                }
                context.source.sender.sendVanillaFeedback(
                    "give.success." + if (singular) "single" else "multiple",
                    Component.text(amount),
                    item.getItemStack().vanillaDisplayName(),
                    if (singular) players[0].name() else Component.text(players.size)
                )
            }

            permission("rebar.command.give")
            executes {
                RebarMetrics.onCommandRun("/rb give")
                givePlayers(this, 1)
            }

            argument("amount", IntegerArgumentType.integer(1)) {
                permission("rebar.command.give")
                executes {
                    RebarMetrics.onCommandRun("/rb give")
                    givePlayers(this, IntegerArgumentType.getInteger(this, "amount"))
                }
            }
        }
    }
}

private val debug = buildCommand("debug") {
    permission("rebar.command.debug")
    executesWithPlayer { player ->
        RebarMetrics.onCommandRun("/rb debug")
        player.inventory.addItem(DebugWaxedWeatheredCutCopperStairs.STACK)
        player.sendVanillaFeedback("give.success.single", Component.text(1), DebugWaxedWeatheredCutCopperStairs.STACK.vanillaDisplayName(), player.name())
    }
}

private val key = buildCommand("key") {
    permission("rebar.command.key")
    executesWithPlayer { player ->
        RebarMetrics.onCommandRun("/rb key")
        val item = RebarItem.fromStack(player.inventory.getItem(EquipmentSlot.HAND))
        if (item == null) {
            player.sendFeedback("key.no_item")
            return@executesWithPlayer
        }
        player.sendMessage(Component.text(item.key.toString())
            .hoverEvent(HoverEvent.showText(Component.translatable("rebar.message.command.key.hover")))
            .clickEvent(ClickEvent.copyToClipboard(item.key.toString())))
    }
}

private val setblock = buildCommand("setblock") {
    argument("pos", ArgumentTypes.blockPosition()) {
        argument("block", RegistryCommandArgument(RebarRegistry.BLOCKS)) {
            permission("rebar.command.setblock")
            executes {
                RebarMetrics.onCommandRun("/rb setblock")
                val location = getArgument<PaperBlockPosition>("pos").toLocation(source.location.world)
                if (!location.world.isPositionLoaded(location)) {
                    source.sender.sendMessage(Component.translatable("argument.pos.unloaded"))
                    return@executes
                } else if (!location.world.worldBorder.isInside(location)) {
                    source.sender.sendMessage(Component.translatable("argument.pos.outofworld"))
                    return@executes
                }

                val block = getArgument<RebarBlockSchema>("block")
                val failed = BlockStorage.isRebarBlock(location)
                        || BlockStorage.placeBlock(location, block.key) == null
                
                source.sender.sendVanillaFeedback(
                    if (failed) "setblock.failed" else "setblock.success",
                    Component.text(location.blockX), Component.text(location.blockY), Component.text(location.blockZ)
                )
            }
        }
    }
}

private val gametest = buildCommand("gametest") {
    argument("pos", ArgumentTypes.blockPosition()) {
        argument("test", RegistryCommandArgument(RebarRegistry.GAMETESTS)) {
            permission("rebar.command.gametest")
            executesWithPlayer { player ->
                RebarMetrics.onCommandRun("/rb gametest")
                val position = BlockPosition(getArgument<PaperBlockPosition>("pos").toLocation(player.world))
                val test = getArgument<GameTestConfig>("test")
                player.sendFeedback(
                    "gametest.started",
                    RebarArgument.of("test", test.key.toString()),
                    RebarArgument.of("location", position.toString())
                )
                Rebar.launch {
                    val result = test.launch(position).await()
                    if (result != null) {
                        player.sendFeedback(
                            "gametest.failed",
                            RebarArgument.of("test", test.key.toString()),
                            RebarArgument.of("reason", result.message ?: "Unknown error")
                        )
                    } else {
                        player.sendFeedback(
                            "gametest.success",
                            RebarArgument.of("test", test.key.toString())
                        )
                    }
                }
            }
        }
    }
}

private val researchAdd = buildCommand("add") {
    argument("players", ArgumentTypes.players()) {
        fun addResearches(context: CommandContext<CommandSourceStack>, researches: List<Research>, confetti: Boolean = true) {
            for (player in context.getArgument<List<Player>>("players")) {
                for (res in researches) {
                    player.addResearch(res, false, confetti)
                    context.source.sender.sendFeedback(
                        "research.added",
                        RebarArgument.of("research", res.name),
                        RebarArgument.of("player", player.name)
                    )
                }
            }
        }

        literal("*") {
            permission("rebar.command.research.add")
            executes {
                // no confetti for all research otherwise server go big boom
                RebarMetrics.onCommandRun("/rb research add")
                addResearches(this, RebarRegistry.RESEARCHES.toList(), false)
            }
        }

        argument("research", RegistryCommandArgument(RebarRegistry.RESEARCHES)) {
            permission("rebar.command.research.add")
            executes {
                RebarMetrics.onCommandRun("/rb research add")
                val res = getArgument<Research>("research")
                addResearches(this, listOf(res))
            }
        }
    }
}

private val researchList = buildCommand("list") {
    fun listResearches(sender: CommandSender, player: Player, type: String) {
        val researches = Research.getResearches(player)
        if (researches.isEmpty()) {
            sender.sendFeedback("research.list.none$type", RebarArgument.of("player", player.name))
            return
        }
        val names = Component.join(JoinConfiguration.commas(true), researches.map(Research::name))
        sender.sendFeedback(
            "research.list.discovered$type",
            RebarArgument.of("player", player.name),
            RebarArgument.of("count", researches.size),
            RebarArgument.of("list", names)
        )
    }

    argument("player", ArgumentTypes.player()) {
        permission("rebar.command.research.list")
        executes { sender ->
            RebarMetrics.onCommandRun("/rb research list")
            val player = getArgument<Player>("player")
            listResearches(sender, player, "_other")
        }
    }
}

private val researchRemove = buildCommand("remove") {
    argument("players", ArgumentTypes.players()) {
        fun removeResearches(context: CommandContext<CommandSourceStack>, researches: List<Research>) {
            for (player in context.getArgument<List<Player>>("players")) {
                for (res in researches) {
                    if (player.hasResearch(res)) {
                        player.removeResearch(res)
                        context.source.sender.sendFeedback(
                            "research.removed",
                            RebarArgument.of("research", res.name),
                            RebarArgument.of("player", player.name)
                        )
                    }
                }
            }
        }

        literal("*") {
            permission("rebar.command.research.remove")
            executes {
                RebarMetrics.onCommandRun("/rb research remove")
                removeResearches(this, RebarRegistry.RESEARCHES.toList())
            }
        }

        argument("research", RegistryCommandArgument(RebarRegistry.RESEARCHES)) {
            permission("rebar.command.research.remove")
            executes {
                RebarMetrics.onCommandRun("/rb research remove")
                val res = getArgument<Research>("research")
                removeResearches(this, listOf(res))
            }
        }
    }
}

private val researchPointsSet = buildCommand("set") {
    argument("players", ArgumentTypes.players()) {
        argument("points", LongArgumentType.longArg(0)) {
            permission("rebar.command.research.points.set")
            executes { sender ->
                RebarMetrics.onCommandRun("/rb research points set")
                val points = getArgument<Long>("points")
                for (player in getArgument<List<Player>>("players")) {
                    player.researchPoints = points
                    sender.sendFeedback(
                        "research.points.set",
                        RebarArgument.of("player", player.name),
                        RebarArgument.of("points", points)
                    )
                }
            }
        }
    }
}

private val researchPointsAdd = buildCommand("add") {
    argument("players", ArgumentTypes.players()) {
        argument("points", LongArgumentType.longArg()) {
            permission("rebar.command.research.points.add")
            executes { sender ->
                RebarMetrics.onCommandRun("/rb research points add")
                val points = getArgument<Long>("points")
                for (player in getArgument<List<Player>>("players")) {
                    player.researchPoints += points
                    sender.sendFeedback(
                        "research.points.added",
                        RebarArgument.of("player", player.name),
                        RebarArgument.of("points", points)
                    )
                }
            }
        }
    }
}

private val researchPointsSubtract = buildCommand("subtract") {
    argument("players", ArgumentTypes.players()) {
        argument("points", LongArgumentType.longArg()) {
            permission("rebar.command.research.points.subtract")
            executes { sender ->
                RebarMetrics.onCommandRun("/rb research points subtract")
                val points = getArgument<Long>("points")
                for (player in getArgument<List<Player>>("players")) {
                    player.researchPoints -= points
                    sender.sendFeedback(
                        "research.points.removed",
                        RebarArgument.of("player", player.name),
                        RebarArgument.of("points", points)
                    )
                }
            }
        }
    }
}

private val researchPointQuery = buildCommand("get") {
    argument("player", ArgumentTypes.player()) {
        permission("rebar.command.research.points.get")
        executes { sender ->
            RebarMetrics.onCommandRun("/rb research points get")
            val player = getArgument<Player>("player")
            val points = player.researchPoints
            sender.sendFeedback(
                "research.points.get",
                RebarArgument.of("player", player.name),
                RebarArgument.of("points", points)
            )
        }
    }
}

private val researchPoints = buildCommand("points") {
    then(researchPointsSet)
    then(researchPointsAdd)
    then(researchPointsSubtract)
    then(researchPointQuery)
}

private val research = buildCommand("research") {
    then(researchAdd)
    then(researchList)
    then(researchRemove)
    then(researchPoints)
}

private val exposeRecipeConfig = buildCommand("exposerecipeconfig") {
    argument("addon", RegistryCommandArgument(RebarRegistry.ADDONS)) {
        argument("recipe", RegistryCommandArgument(RebarRegistry.RECIPE_TYPES)) {
            permission("rebar.command.exposerecipeconfig")
            executes { sender ->
                RebarMetrics.onCommandRun("/rb exposerecipeconfig")
                val addon = getArgument<RebarAddon>("addon")
                val recipeType = getArgument<RecipeType<*>>("recipe")
                if (recipeType !is ConfigurableRecipeType) {
                    sender.sendFeedback("exposerecipe.not-configurable")
                    return@executes
                }
                sender.sendFeedback(
                    "exposerecipe.warning",
                    RebarArgument.of("file", "plugins/rbbar/${recipeType.filePath}")
                )
                mergeGlobalConfig(addon, recipeType.filePath, recipeType.filePath)
            }
        }
    }
}

private val confetti = buildCommand("confetti") {
    argument("amount", IntegerArgumentType.integer(1)) {
        permission("rebar.command.confetti")
        executesWithPlayer { player ->
            RebarMetrics.onCommandRun("/rb confetti")
            ConfettiParticle.spawnMany(player.location, IntegerArgumentType.getInteger(this, "amount")).run()
        }
        argument("speed", DoubleArgumentType.doubleArg(0.0)) {
            permission("rebar.command.confetti")
            executesWithPlayer { player ->
                RebarMetrics.onCommandRun("/rb confetti")
                ConfettiParticle.spawnMany(player.location, IntegerArgumentType.getInteger(this, "amount"), DoubleArgumentType.getDouble(this, "speed")).run()
            }
            argument("lifetime", IntegerArgumentType.integer(1)) {
                permission("rebar.command.confetti")
                executesWithPlayer { player ->
                    RebarMetrics.onCommandRun("/rb confetti")
                    ConfettiParticle.spawnMany(
                        player.location,
                        IntegerArgumentType.getInteger(this, "amount"),
                        DoubleArgumentType.getDouble(this, "speed"),
                        IntegerArgumentType.getInteger(this, "lifetime")
                    ).run()
                }
            }
        }
    }
}

private val setphantom = buildCommand("setphantom") {
    argument("pos", ArgumentTypes.blockPosition()) {
        permission("rebar.command.setphantom")
        executes { sender ->
            RebarMetrics.onCommandRun("/rb setphantom")
            val position = getArgument<PaperBlockPosition>("pos").toLocation(source.location.world)
            if (!position.world.isPositionLoaded(position)) {
                source.sender.sendMessage(Component.translatable("argument.pos.unloaded"))
                return@executes
            } else if (!position.world.worldBorder.isInside(position)) {
                source.sender.sendMessage(Component.translatable("argument.pos.outofworld"))
                return@executes
            }

            val block = BlockStorage.get(position)
            if (block == null) {
                source.sender.sendVanillaFeedback("setblock.failed", Component.text(position.blockX), Component.text(position.blockY), Component.text(position.blockZ))
                return@executes
            }

            BlockStorage.makePhantom(block)
            source.sender.sendVanillaFeedback("setblock.success", Component.text(position.blockX), Component.text(position.blockY), Component.text(position.blockZ))
        }
    }
}

@JvmSynthetic
internal val ROOT_COMMAND = buildCommand("rebar") {
    permission("rebar.command.guide")
    executesWithPlayer { player ->
        RebarMetrics.onCommandRun("/rb")
        RebarGuide.open(player)
    }

    then(guide)
    then(give)
    then(debug)
    then(key)
    then(setblock)
    then(setphantom)
    then(gametest)
    then(research)
    then(exposeRecipeConfig)
    then(confetti)
}

@JvmSynthetic
internal val ROOT_COMMAND_RE_ALIAS = buildCommand("rb") {
    redirect(ROOT_COMMAND)
}

@JvmSynthetic
@Suppress("UnstableApiUsage")
inline fun <reified T> CommandContext<CommandSourceStack>.getArgument(name: String): T {
    return when (typeOf<T>()) {
        typeOf<PaperBlockPosition>() -> getArgument(name, BlockPositionResolver::class.java).resolve(source)
        typeOf<List<Entity>>() -> getArgument(name, EntitySelectorArgumentResolver::class.java).resolve(source)
        typeOf<Entity>() -> getArgument(name, EntitySelectorArgumentResolver::class.java).resolve(source).first()
        typeOf<FinePosition>() -> getArgument(name, FinePositionResolver::class.java).resolve(source)
        typeOf<List<PlayerProfile>>() -> getArgument(name, PlayerProfileListResolver::class.java).resolve(source)
        typeOf<PlayerProfile>() -> getArgument(name, PlayerProfileListResolver::class.java).resolve(source).first()
        typeOf<List<Player>>() -> getArgument(name, PlayerSelectorArgumentResolver::class.java).resolve(source)
        typeOf<Player>() -> getArgument(name, PlayerSelectorArgumentResolver::class.java).resolve(source).first()
        typeOf<Rotation>() -> getArgument(name, RotationResolver::class.java).resolve(source)
        else -> getArgument(name, T::class.java)
    } as T
}

private fun CommandSender.sendFeedback(key: String, vararg args: ComponentLike) {
    sendMessage(Component.translatable("rebar.message.command.$key").arguments(*args))
}

private fun CommandSender.sendVanillaFeedback(key: String, vararg args: ComponentLike) {
    sendMessage(Component.translatable("commands.$key", *args))
}