@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.block.*
import io.github.pylonmc.rebar.block.base.*
import io.github.pylonmc.rebar.block.base.RebarFallingBlock.RebarFallingBlockEntity
import io.github.pylonmc.rebar.command.ROOT_COMMAND
import io.github.pylonmc.rebar.command.ROOT_COMMAND_RE_ALIAS
import io.github.pylonmc.rebar.config.Config
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.cargo.CargoDuct
import io.github.pylonmc.rebar.content.debug.DebugWaxedWeatheredCutCopperStairs
import io.github.pylonmc.rebar.content.fluid.*
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.entity.EntityListener
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.base.RebarTickingEntity
import io.github.pylonmc.rebar.fluid.placement.FluidPipePlacementService
import io.github.pylonmc.rebar.guide.pages.base.PagedGuidePage
import io.github.pylonmc.rebar.guide.pages.base.TabbedGuidePage
import io.github.pylonmc.rebar.i18n.RebarTranslator
import io.github.pylonmc.rebar.item.RebarInventoryTicker
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.logistics.CargoRoutes
import io.github.pylonmc.rebar.metrics.RebarMetrics
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.RebarRecipeListener
import io.github.pylonmc.rebar.recipe.RecipeCompletion
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.resourcepack.armor.ArmorTextureEngine
import io.github.pylonmc.rebar.resourcepack.block.BlockTextureEngine
import io.github.pylonmc.rebar.util.mergeGlobalConfig
import io.github.pylonmc.rebar.waila.Waila
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import io.papermc.paper.ServerBuildInfo
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.delay
import me.tofaa.entitylib.APIConfig
import me.tofaa.entitylib.EntityIdProvider
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Display
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.ItemDisplay
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.invui.InvUI
import xyz.xenondevs.invui.i18n.Languages
import java.util.Locale
import kotlin.io.path.*

/**
 * The one and only Rebar plugin!
 */
object Rebar : JavaPlugin(), RebarAddon {

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        val start = System.currentTimeMillis()

        val expectedVersion = pluginMeta.apiVersion
        val actualVersion = ServerBuildInfo.buildInfo().minecraftVersionId()
        if (actualVersion != expectedVersion) {
            logger.severe("!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!")
            logger.severe("You are running Rebar on Minecraft version $actualVersion")
            logger.severe("This build of Rebar expects Minecraft version $expectedVersion")
            logger.severe("Rebar may run fine, but you may encounter bugs ranging from mild to catastrophic")
            logger.severe("Please update your Rebar version accordingly")
            logger.severe("Please see https://github.com/pylonmc/rebar/releases for available Rebar versions")
            logger.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        }

        InvUI.getInstance().setPlugin(this)
        Languages.getInstance().enableServerSideTranslations(false) // we do our own

        val packetEvents = PacketEvents.getAPI()
        packetEvents.init()

        val entityLibPlatform = SpigotEntityLibPlatform(this)
        val entityLibSettings = APIConfig(packetEvents).tickTickables()
        EntityLib.init(entityLibPlatform, entityLibSettings)
        entityLibPlatform.entityIdProvider = EntityIdProvider { _, _ ->
            @Suppress("DEPRECATION")
            Bukkit.getUnsafe().nextEntityId()
        }

        saveDefaultConfig()
        // Add any keys that are missing from global config - saveDefaultConfig will not do anything if config already present
        mergeGlobalConfig(Rebar, "config.yml", "config.yml")

        val pm = Bukkit.getPluginManager()
        pm.registerEvents(RebarTranslator, this)
        pm.registerEvents(RebarAddon, this)

        RebarMetrics // initialize metrics by referencing it

        // Anything that listens for addon registration must be above this line
        registerWithRebar()

        pm.registerEvents(BlockStorage, this)
        pm.registerEvents(BlockListener, this)
        pm.registerEvents(RebarCopperBlock, this)
        pm.registerEvents(RebarItemListener, this)
        pm.registerEvents(MultiblockCache, this)
        pm.registerEvents(EntityStorage, this)
        pm.registerEvents(EntityListener, this)
        pm.registerEvents(Research, this)
        pm.registerEvents(RebarVirtualInventoryBlock, this)
        pm.registerEvents(RebarGuiBlock, this)
        pm.registerEvents(RebarEntityHolderBlock, this)
        pm.registerEvents(RebarSimpleMultiblock, this)
        pm.registerEvents(RebarProcessor, this)
        pm.registerEvents(RebarRecipeProcessor, this)
        pm.registerEvents(RebarFluidBufferBlock, this)
        pm.registerEvents(RebarFluidTank, this)
        pm.registerEvents(RebarRecipeListener, this)
        pm.registerEvents(RebarDirectionalBlock, this)
        pm.registerEvents(FluidPipePlacementService, this)
        pm.registerEvents(RebarTickingBlock, this)
        pm.registerEvents(RebarGuide, this)
        pm.registerEvents(RebarLogisticBlock, this)
        pm.registerEvents(RebarCargoBlock, this)
        pm.registerEvents(CargoRoutes, this)
        pm.registerEvents(CargoDuct, this)
        pm.registerEvents(RecipeCompletion, this)
        pm.registerEvents(PagedGuidePage, this)
        pm.registerEvents(TabbedGuidePage, this)
        pm.registerEvents(RebarTickingEntity, this)

        Bukkit.getScheduler().runTaskTimer(this, RebarInventoryTicker(), 0, RebarConfig.INVENTORY_TICKER_BASE_RATE)

        if (RebarConfig.WailaConfig.enabled) {
            pm.registerEvents(Waila, this)
        }

        if (RebarConfig.ArmorTextureConfig.ENABLED) {
            packetEvents.eventManager.registerListener(ArmorTextureEngine, PacketListenerPriority.HIGHEST)
        }

        if (RebarConfig.BlockTextureConfig.ENABLED) {
            pm.registerEvents(BlockTextureEngine, this)
            BlockTextureEngine.updateOccludingCacheJob.start()
            BlockTextureEngine.syncCullingJob.start()
        }

        Bukkit.getScheduler().runTaskTimer(
            this,
            MultiblockCache.MultiblockChecker,
            MultiblockCache.MultiblockChecker.INTERVAL_TICKS,
            MultiblockCache.MultiblockChecker.INTERVAL_TICKS
        )

        addDefaultPermission("rebar.command.guide")
        addDefaultPermission("rebar.command.waila")
        addDefaultPermission("rebar.command.research.list.self")
        addDefaultPermission("rebar.command.research.discover")
        addDefaultPermission("rebar.command.research.points.query.self")
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(ROOT_COMMAND)
            it.registrar().register(ROOT_COMMAND_RE_ALIAS)
        }

        RebarItem.register<DebugWaxedWeatheredCutCopperStairs>(DebugWaxedWeatheredCutCopperStairs.STACK)
        RebarGuide.hideItem(DebugWaxedWeatheredCutCopperStairs.KEY)

        RebarItem.register<PhantomBlock.ErrorItem>(PhantomBlock.ErrorItem.STACK)
        RebarGuide.hideItem(PhantomBlock.ErrorItem.KEY)

        RebarItem.register<RebarGuide>(RebarGuide.STACK)
        RebarGuide.hideItem(RebarGuide.KEY)

        RebarEntity.register<Display, RebarSimpleMultiblock.MultiblockGhostBlock>(
            RebarSimpleMultiblock.MultiblockGhostBlock.KEY,
        )

        RebarEntity.register<ItemDisplay, FluidEndpointDisplay>(FluidEndpointDisplay.KEY)
        RebarEntity.register<ItemDisplay, FluidIntersectionDisplay>(FluidIntersectionDisplay.KEY)
        RebarEntity.register<ItemDisplay, FluidPipeDisplay>(FluidPipeDisplay.KEY)

        RebarEntity.register<FallingBlock, RebarFallingBlockEntity>(RebarFallingBlock.KEY)

        RebarBlock.register<FluidSectionMarker>(FluidSectionMarker.KEY, Material.STRUCTURE_VOID)
        RebarBlock.register<FluidIntersectionMarker>(FluidIntersectionMarker.KEY, Material.STRUCTURE_VOID)

        RecipeType.addVanillaRecipes()

        launch {
            delay(1.ticks)
            loadRecipes()
            loadResearches()
        }

        val end = System.currentTimeMillis()
        logger.info("Loaded in ${(end - start) / 1000.0}s")
    }

    private fun loadRecipes() {
        val start = System.currentTimeMillis()

        logger.info("Loading recipes...")
        for (type in RebarRegistry.RECIPE_TYPES) {
            if (type !is ConfigurableRecipeType) continue
            for (addon in RebarRegistry.ADDONS) {
                val configStream = addon.javaPlugin.getResource(type.filePath) ?: continue
                val config = configStream.reader().use { ConfigSection(YamlConfiguration.loadConfiguration(it)) }
                type.loadFromConfig(config)
            }
        }

        val recipesDir = dataPath.resolve("recipes")
        if (recipesDir.exists()) {
            for (recipeDir in recipesDir.listDirectoryEntries()) {
                if (!recipeDir.isDirectory()) continue
                val namespace = recipeDir.nameWithoutExtension
                for (recipe in recipeDir.listDirectoryEntries()) {
                    if (!recipe.isRegularFile() || recipe.extension != "yml") continue
                    val key = NamespacedKey(namespace, recipe.nameWithoutExtension)
                    val type = RebarRegistry.RECIPE_TYPES[key] as? ConfigurableRecipeType ?: continue
                    type.loadFromConfig(Config(recipe))
                }
            }
        }

        val end = System.currentTimeMillis()
        logger.info("Loaded recipes in ${(end - start) / 1000.0}s")
    }

    private fun loadResearches() {
        logger.info("Loading researches...")
        val start = System.currentTimeMillis()

        for (addon in RebarRegistry.ADDONS) {
            mergeGlobalConfig(addon, "researches.yml", "researches/${addon.key.namespace}.yml", false)
        }

        val researchDir = dataPath.resolve("researches")
        if (researchDir.exists()) {
            for (namespaceDir in researchDir.listDirectoryEntries()) {
                val namespace = namespaceDir.nameWithoutExtension

                if (!namespaceDir.isRegularFile()) continue

                val mainResearchConfig = Config(namespaceDir)
                for (key in mainResearchConfig.keys) {
                    val nsKey = NamespacedKey(namespace, key)
                    val section = mainResearchConfig.getSection(key) ?: continue

                    Research.loadFromConfig(section, nsKey).register()
                }
            }
        }

        val end = System.currentTimeMillis()
        logger.info("Loaded researches in ${(end - start) / 1000.0}s")
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
        FluidPipePlacementService.cleanup()
        BlockStorage.cleanupEverything()
        EntityStorage.cleanupEverything()
        RebarMetrics.save()
    }

    override val javaPlugin = this

    override val material = Material.BEDROCK

    override val languages: Set<Locale> = setOf(
        Locale.ENGLISH,
        Locale.of("enws")
    )
}

private fun addDefaultPermission(permission: String) {
    Bukkit.getPluginManager().addPermission(Permission(permission, PermissionDefault.TRUE))
}