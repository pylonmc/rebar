@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.async.BukkitMainThreadDispatcher
import io.github.pylonmc.rebar.async.ChunkScope
import io.github.pylonmc.rebar.async.PlayerScope
import io.github.pylonmc.rebar.block.*
import io.github.pylonmc.rebar.block.interfaces.*
import io.github.pylonmc.rebar.command.ROOT_COMMAND
import io.github.pylonmc.rebar.command.ROOT_COMMAND_RE_ALIAS
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.cargo.CargoDuct
import io.github.pylonmc.rebar.content.debug.DebugWaxedWeatheredCutCopperStairs
import io.github.pylonmc.rebar.content.fluid.*
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.culling.BlockCullingEngine
import io.github.pylonmc.rebar.entity.ConfettiCreeperListener
import io.github.pylonmc.rebar.entity.EntityListener
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.interfaces.*
import io.github.pylonmc.rebar.event.RebarConfigurableRecipesLoadedEvent
import io.github.pylonmc.rebar.fluid.placement.FluidPipePlacementService
import io.github.pylonmc.rebar.guide.pages.base.PagedGuidePage
import io.github.pylonmc.rebar.guide.pages.base.TabbedGuidePage
import io.github.pylonmc.rebar.i18n.CreativeActionTranslationHandler
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
import io.github.pylonmc.rebar.util.delayTicks
import io.github.pylonmc.rebar.item.interfaces.*
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.util.mergeResource
import io.github.pylonmc.rebar.waila.Waila
import io.github.pylonmc.rebar.waila.WailaPlaceholders
import io.papermc.paper.ServerBuildInfo
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.ApiStatus
import xyz.xenondevs.invui.InvUI
import xyz.xenondevs.invui.i18n.Languages
import java.util.*
import kotlin.io.path.*

/**
 * The one and only Rebar plugin!
 */
object Rebar : JavaPlugin(), RebarAddon {

    /**
     * Ticks once per tick
     */
    @get:JvmSynthetic
    @get:ApiStatus.Internal
    val mainThreadDispatcher by lazy { BukkitMainThreadDispatcher(this, 1) }

    /**
     * By default, dispatches on the main thread
     */
    @get:JvmSynthetic
    @get:ApiStatus.Internal
    val scope by lazy { CoroutineScope(SupervisorJob() + mainThreadDispatcher) }

    override fun onEnable() {
        val start = System.currentTimeMillis()

        val expectedVersion = pluginMeta.apiVersion
        val actualVersion = ServerBuildInfo.buildInfo().minecraftVersionId()
        if (actualVersion != expectedVersion) {
            logger.severe("!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!")
            logger.severe("You are running Rebar on Minecraft version $actualVersion")
            logger.severe("This build of Rebar expects Minecraft version $expectedVersion")
            logger.severe("Please see https://github.com/pylonmc/rebar/releases for available Rebar versions")
            try {
                if (RebarConfig.BYPASS_VERSION_CHECK) {
                    logger.severe("Bypass version set to true in config; proceeding anyway")
                } else {
                    logger.severe("Rebar will refuse to start")
                    logger.severe("You can attempt to start anyway by setting bypass-version-check to true in the config")
                }
            } catch (e: Exception){
                throw RuntimeException("Error while getting value of bypass-version-check", e)
            }
            logger.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            if (!RebarConfig.BYPASS_VERSION_CHECK) {
                throw RuntimeException("The server is running $actualVersion but Rebar expected $expectedVersion")
            }
        }

        InvUI.getInstance().setPlugin(this)
        Languages.getInstance().enableServerSideTranslations(false) // we do our own

        val pm = Bukkit.getPluginManager()
        pm.registerEvents(RebarTranslator, this)
        pm.registerEvents(RebarAddon, this)

        RebarMetrics // initialize metrics by referencing it

        // Anything that listens for addon registration must be above this line
        registerWithRebar()

        pm.registerEvents(ChunkScope, this)
        pm.registerEvents(PlayerScope, this)

        pm.registerEvents(EntityStorage, this)
        pm.registerEvents(BlockStorage, this)
        pm.registerEvents(MultiblockCache, this)

        pm.registerEvents(RebarItemListener, this)
        pm.registerEvents(RebarRecipeListener, this)
        pm.registerEvents(RecipeCompletion, this)
        pm.registerEvents(CreativeActionTranslationHandler, this)
        pm.registerEvents(Research, this)

        EntityListener.register(this, pm)
        BlockListener.register(this, pm)

        pm.registerEvents(RebarGuide, this)
        pm.registerEvents(PagedGuidePage, this)
        pm.registerEvents(TabbedGuidePage, this)

        pm.registerEvents(FluidPipePlacementService, this)

        pm.registerEvents(CargoRoutes, this)
        pm.registerEvents(CargoDuct, this)

        ConfettiCreeperListener.register(this, pm)

        // Rebar Blocks
        pm.registerEvents(CargoRebarBlock, this)
        pm.registerEvents(DirectionalRebarBlock, this)
        pm.registerEvents(EntityHolderRebarBlock, this)
        pm.registerEvents(FluidBufferRebarBlock, this)
        pm.registerEvents(FluidTankRebarBlock, this)
        pm.registerEvents(GhostBlockHolderRebarBlock, this)
        pm.registerEvents(GuiRebarBlock, this)
        pm.registerEvents(LogisticRebarBlock, this)
        pm.registerEvents(ProcessorRebarBlock, this)
        pm.registerEvents(RecipeProcessorRebarBlock, this)
        pm.registerEvents(SimpleRebarMultiblock, this)
        pm.registerEvents(TickingRebarBlock, this)
        pm.registerEvents(VirtualInventoryRebarBlock, this)
        pm.registerEvents(FallingRebarBlockHandler, this)
        BeaconRebarBlockHandler.register(this, pm)
        BedRebarBlockHandler.register(this, pm)
        BellRebarBlockHandler.register(this, pm)
        BrewingStandRebarBlockHandler.register(this, pm)
        CampfireRebarBlockHandler.register(this, pm)
        CargoRebarBlockHandler.register(this, pm)
        CauldronRebarBlockHandler.register(this, pm)
        ComposterRebarBlockHandler.register(this, pm)
        CopperRebarBlockHandler.register(this, pm)
        CrafterRebarBlockHandler.register(this, pm)
        DispenserRebarBlockHandler.register(this, pm)
        EnchantingTableRebarBlockHandler.register(this, pm)
        EntityChangeRebarBlockHandler.register(this, pm)
        FireRebarBlockHandler.register(this, pm)
        FlowerPotRebarBlockHandler.register(this, pm)
        FurnaceRebarBlockHandler.register(this, pm)
        GrowRebarBlockHandler.register(this, pm)
        HopperRebarBlockHandler.register(this, pm)
        InteractRebarBlockHandler.register(this, pm)
        JobRebarBlockHandler.register(this, pm)
        JumpRebarBlockHandler.register(this, pm)
        LeafRebarBlockHandler.register(this, pm)
        LecternRebarBlockHandler.register(this, pm)
        LootDispenserRebarBlockHandler.register(this, pm)
        NoteRebarBlockHandler.register(this, pm)
        PistonRebarBlockHandler.register(this, pm)
        RedstoneRebarBlockHandler.register(this, pm)
        ShearRebarBlockHandler.register(this, pm)
        SignRebarBlockHandler.register(this, pm)
        SneakRebarBlockHandler.register(this, pm)
        SpongeRebarBlockHandler.register(this, pm)
        TargetRebarBlockHandler.register(this, pm)
        TNTRebarBlockHandler.register(this, pm)
        UnloadRebarBlockHandler.register(this, pm)
        VanillaInventoryRebarBlockHandler.register(this, pm)
        VaultRebarBlockHandler.register(this, pm)

        // Rebar Items
        AnvilUseRebarItemHandler.register(this, pm)
        ArrowRebarItemHandler.register(this, pm)
        BlockBreakRebarItemHandler.register(this, pm)
        BlockInteractRebarItemHandler.register(this, pm)
        BottleRebarItemHandler.register(this, pm)
        BowRebarItemHandler.register(this, pm)
        BrewingStandFuelRebarItemHandler.register(this, pm)
        BucketRebarItemHandler.register(this, pm)
        ConsumeRebarItemHandler.register(this, pm)
        DispenseRebarItemHandler.register(this, pm)
        DropRebarItemHandler.register(this, pm)
        DurabilityRebarItemHandler.register(this, pm)
        EntityAttackRebarItemHandler.register(this, pm)
        EntityInteractRebarItemHandler.register(this, pm)
        FurnaceBurnRebarItemHandler.register(this, pm)
        InteractRebarItemHandler.register(this, pm)
        JoinRebarItemHandler.register(this, pm)
        LingeringPotionRebarItemHandler.register(this, pm)
        PickupRebarItemHandler.register(this, pm)
        ProjectileRebarItemHandler.register(this, pm)
        SplashPotionRebarItemHandler.register(this, pm)

        // Rebar Entities
        pm.registerEvents(TickingRebarEntity, this)
        BatRebarEntityHandler.register(this, pm)
        BreakDoorRebarEntityHandler.register(this, pm)
        BreedRebarEntityHandler.register(this, pm)
        CombustRebarEntityHandler.register(this, pm)
        CreeperRebarEntityHandler.register(this, pm)
        DamageRebarEntityHandler.register(this, pm)
        DeathRebarEntityHandler.register(this, pm)
        DragonFireballRebarEntityHandler.register(this, pm)
        DyeRebarEntityHandler.register(this, pm)
        EnderDragonRebarEntityHandler.register(this, pm)
        EndermanRebarEntityHandler.register(this, pm)
        ExperienceOrbRebarEntityHandler.register(this, pm)
        ExplosiveRebarEntityHandler.register(this, pm)
        FireworkRebarEntityHandler.register(this, pm)
        InteractRebarEntityHandler.register(this, pm)
        ItemRebarEntityHandler.register(this, pm)
        LeashRebarEntityHandler.register(this, pm)
        MountRebarEntityHandler.register(this, pm)
        MoveRebarEntityHandler.register(this, pm)
        PassengerRebarEntityHandler.register(this, pm)
        PathfindRebarEntityHandler.register(this, pm)
        PiglinRebarEntityHandler.register(this, pm)
        ProjectileRebarEntityHandler.register(this, pm)
        ResurrectRebarEntityHandler.register(this, pm)
        SlimeRebarEntityHandler.register(this, pm)
        SpellcasterRebarEntityHandler.register(this, pm)
        TameRebarEntityHandler.register(this, pm)
        TargetEntityRebarEntityHandler.register(this, pm)
        TurtleRebarEntityHandler.register(this, pm)
        UnloadRebarEntityHandler.register(this, pm)
        VillagerRebarEntityHandler.register(this, pm)
        WitchRebarEntityHandler.register(this, pm)
        ZombifiedPiglinRebarEntityHandler.register(this, pm)

        Bukkit.getScheduler().runTaskTimer(this, RebarInventoryTicker(), 0, RebarConfig.INVENTORY_TICKER_BASE_RATE)

        if (RebarConfig.WailaConfig.ENABLED) {
            pm.registerEvents(Waila, this)
            if (pm.getPlugin("PlaceholderAPI") != null) {
                WailaPlaceholders.register()
            }
        }

        if (RebarConfig.BlockTextureConfig.ENABLED) {
            pm.registerEvents(BlockCullingEngine, this)
            BlockCullingEngine.invalidateOccludingCacheJob.start()
            BlockCullingEngine.syncCullingJob.start()
        }

        Bukkit.getScheduler().runTaskTimer(
            this,
            MultiblockCache.MultiblockChecker,
            MultiblockCache.MultiblockChecker.INTERVAL_TICKS,
            MultiblockCache.MultiblockChecker.INTERVAL_TICKS
        )

        addDefaultPermission("rebar.command.guide")
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

        RebarEntity.register<Interaction, GhostBlockHolderRebarBlock.GhostBlockHitbox>(
            GhostBlockHolderRebarBlock.GhostBlockHitbox.KEY
        )

        RebarEntity.register<BlockDisplay, GhostBlockHolderRebarBlock.VanillaGhostBlock>(
            GhostBlockHolderRebarBlock.VanillaGhostBlock.KEY
        )

        RebarEntity.register<ItemDisplay, GhostBlockHolderRebarBlock.RebarGhostBlock>(
            GhostBlockHolderRebarBlock.RebarGhostBlock.KEY
        )

        RebarEntity.register<ItemDisplay, FluidEndpointDisplay>(FluidEndpointDisplay.KEY)
        RebarEntity.register<ItemDisplay, FluidIntersectionDisplay>(FluidIntersectionDisplay.KEY)
        RebarEntity.register<ItemDisplay, FluidPipeDisplay>(FluidPipeDisplay.KEY)

        RebarEntity.register<FallingBlock, FallingRebarBlockHandler.RebarFallingBlockEntity>(FallingRebarBlockHandler.KEY)

        RebarBlock.register<FluidSectionMarker>(FluidSectionMarker.KEY, Material.STRUCTURE_VOID)
        RebarBlock.register<FluidIntersectionMarker>(FluidIntersectionMarker.KEY, Material.STRUCTURE_VOID)

        RecipeType.addVanillaRecipes()

        scope.launch(mainThreadDispatcher) {
            delayTicks(1)
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
                val config = ConfigSection.fromResource(addon.javaPlugin, type.filePath) ?: continue
                type.loadFromConfig(config)
            }
        }

        val recipesDir = dataPath.resolve("recipes")
        if (recipesDir.exists()) {
            for (recipeDir in recipesDir.listDirectoryEntries()) {
                if (!recipeDir.isDirectory()) continue
                val namespace = recipeDir.nameWithoutExtension
                for (recipePath in recipeDir.listDirectoryEntries()) {
                    if (!recipePath.isRegularFile() || recipePath.extension != "yml" || recipePath.extension != "yaml") continue
                    val key = NamespacedKey(namespace, recipePath.nameWithoutExtension)
                    val type = RebarRegistry.RECIPE_TYPES[key] as? ConfigurableRecipeType ?: continue
                    type.loadFromConfig(ConfigSection.fromOrThrow(recipePath))
                }
            }
        }

        NmsAccessor.processRecipeQueue()
        val end = System.currentTimeMillis()
        logger.info("Loaded recipes in ${(end - start) / 1000.0}s")
        RebarConfigurableRecipesLoadedEvent().callEvent()
    }

    private fun loadResearches() {
        logger.info("Loading researches...")
        val start = System.currentTimeMillis()

        for (addon in RebarRegistry.ADDONS) {
            mergeResource(addon, "researches.yml", "researches/${addon.key.namespace}.yml", false)
        }

        val researchDir = dataPath.resolve("researches")
        if (researchDir.exists()) {
            for (namespaceDir in researchDir.listDirectoryEntries()) {
                val namespace = namespaceDir.nameWithoutExtension

                if (!namespaceDir.isRegularFile()) continue

                val mainResearchConfig = ConfigSection.fromOrThrow(namespaceDir)
                for (key in mainResearchConfig.keys) {
                    val nsKey = NamespacedKey(namespace, key)
                    val section = mainResearchConfig.getSection(key) ?: continue

                    Research.loadFromConfig(section, nsKey)?.register()
                }
            }
        }

        val end = System.currentTimeMillis()
        logger.info("Loaded researches in ${(end - start) / 1000.0}s")
    }

    @JvmSynthetic
    internal fun preDisable() {
        // Anything that requires listeners to still be registered should be done here
        FluidPipePlacementService.cleanup()
        BlockStorage.cleanupEverything()
        EntityStorage.cleanupEverything()
    }

    override fun onDisable() {
        // Note: At this point all listeners have been unregistered
        RebarMetrics.save()
        scope.cancel()
    }

    override val javaPlugin = this

    override val material = Material.BEDROCK

    override val defaultLanguage: Locale = RebarConfig.DEFAULT_LANGUAGE
}

private fun addDefaultPermission(permission: String) {
    Bukkit.getPluginManager().addPermission(Permission(permission, PermissionDefault.TRUE))
}