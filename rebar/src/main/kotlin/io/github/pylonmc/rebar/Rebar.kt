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
import io.github.pylonmc.rebar.electricity.WireConnectionService
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
import io.github.pylonmc.rebar.item.interfaces.*
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.logistics.CargoRoutes
import io.github.pylonmc.rebar.metrics.RebarMetrics
import io.github.pylonmc.rebar.recipe.ConfigurableRecipeType
import io.github.pylonmc.rebar.recipe.RebarRecipeListener
import io.github.pylonmc.rebar.recipe.RecipeCompletion
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.delayTicks
import io.github.pylonmc.rebar.util.mergeResource
import io.github.pylonmc.rebar.waila.Waila
import io.github.pylonmc.rebar.waila.WailaPlaceholders
import io.github.pylonmc.rebar.world.WorldStorage
import io.papermc.paper.ServerBuildInfo
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
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
            logger.severe("Rebar may run fine, but you may encounter bugs ranging from mild to catastrophic")
            logger.severe("Please update your Rebar version accordingly")
            logger.severe("Please see https://github.com/pylonmc/rebar/releases for available Rebar versions")
            logger.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        }

        InvUI.getInstance().setPlugin(this)
        Languages.getInstance().enableServerSideTranslations(false) // we do our own

        val pm = Bukkit.getPluginManager()
        pm.registerEvents(RebarTranslator, this)
        pm.registerEvents(RebarAddon, this)

        @Suppress("UnusedExpression")
        RebarMetrics // initialize metrics by referencing it

        // Anything that listens for addon registration must be above this line
        registerWithRebar()

        pm.registerEvents(ChunkScope, this)
        pm.registerEvents(PlayerScope, this)

        pm.registerEvents(EntityStorage, this)
        pm.registerEvents(BlockStorage, this)
        pm.registerEvents(MultiblockCache, this)
        pm.registerEvents(WorldStorage, this)

        pm.registerEvents(RebarItemListener, this)
        pm.registerEvents(RebarRecipeListener, this)
        pm.registerEvents(RecipeCompletion, this)
        pm.registerEvents(CreativeActionTranslationHandler, this)
        pm.registerEvents(Research, this)

        EntityListener.register(this)
        BlockListener.register(this)

        pm.registerEvents(RebarGuide, this)
        pm.registerEvents(PagedGuidePage, this)
        pm.registerEvents(TabbedGuidePage, this)

        pm.registerEvents(FluidPipePlacementService, this)
        pm.registerEvents(WireConnectionService, this)

        pm.registerEvents(CargoRoutes, this)
        pm.registerEvents(CargoDuct, this)

        ConfettiCreeperListener.register(this)

        // Rebar Blocks
        pm.registerEvents(CargoRebarBlock, this)
        pm.registerEvents(DirectionalRebarBlock, this)
        pm.registerEvents(ElectricRebarBlock, this)
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
        BeaconRebarBlockHandler.register(this)
        BedRebarBlockHandler.register(this)
        BellRebarBlockHandler.register(this)
        BrewingStandRebarBlockHandler.register(this)
        CampfireRebarBlockHandler.register(this)
        CargoRebarBlockHandler.register(this)
        CauldronRebarBlockHandler.register(this)
        ComposterRebarBlockHandler.register(this)
        CopperRebarBlockHandler.register(this)
        CrafterRebarBlockHandler.register(this)
        DispenserRebarBlockHandler.register(this)
        EnchantingTableRebarBlockHandler.register(this)
        EntityChangeRebarBlockHandler.register(this)
        FireRebarBlockHandler.register(this)
        FlowerPotRebarBlockHandler.register(this)
        FurnaceRebarBlockHandler.register(this)
        GrowRebarBlockHandler.register(this)
        HopperRebarBlockHandler.register(this)
        InteractRebarBlockHandler.register(this)
        JobRebarBlockHandler.register(this)
        JumpRebarBlockHandler.register(this)
        LeafRebarBlockHandler.register(this)
        LecternRebarBlockHandler.register(this)
        LootDispenserRebarBlockHandler.register(this)
        NoteRebarBlockHandler.register(this)
        PistonRebarBlockHandler.register(this)
        RedstoneRebarBlockHandler.register(this)
        ShearRebarBlockHandler.register(this)
        SignRebarBlockHandler.register(this)
        SneakRebarBlockHandler.register(this)
        SpongeRebarBlockHandler.register(this)
        TargetRebarBlockHandler.register(this)
        TNTRebarBlockHandler.register(this)
        UnloadRebarBlockHandler.register(this)
        VanillaInventoryRebarBlockHandler.register(this)
        VaultRebarBlockHandler.register(this)

        // Rebar Items
        AnvilUseRebarItemHandler.register(this)
        ArrowRebarItemHandler.register(this)
        BlockBreakRebarItemHandler.register(this)
        BlockInteractRebarItemHandler.register(this)
        BottleRebarItemHandler.register(this)
        BowRebarItemHandler.register(this)
        BrewingStandFuelRebarItemHandler.register(this)
        BucketRebarItemHandler.register(this)
        ConsumeRebarItemHandler.register(this)
        DispenseRebarItemHandler.register(this)
        DropRebarItemHandler.register(this)
        DurabilityRebarItemHandler.register(this)
        EntityAttackRebarItemHandler.register(this)
        EntityInteractRebarItemHandler.register(this)
        FurnaceBurnRebarItemHandler.register(this)
        InteractRebarItemHandler.register(this)
        JoinRebarItemHandler.register(this)
        LingeringPotionRebarItemHandler.register(this)
        PickupRebarItemHandler.register(this)
        ProjectileRebarItemHandler.register(this)
        SplashPotionRebarItemHandler.register(this)

        // Rebar Entities
        pm.registerEvents(TickingRebarEntity, this)
        BatRebarEntityHandler.register(this)
        BreakDoorRebarEntityHandler.register(this)
        BreedRebarEntityHandler.register(this)
        CombustRebarEntityHandler.register(this)
        CreeperRebarEntityHandler.register(this)
        DamageRebarEntityHandler.register(this)
        DeathRebarEntityHandler.register(this)
        DragonFireballRebarEntityHandler.register(this)
        DyeRebarEntityHandler.register(this)
        EnderDragonRebarEntityHandler.register(this)
        EndermanRebarEntityHandler.register(this)
        ExperienceOrbRebarEntityHandler.register(this)
        ExplosiveRebarEntityHandler.register(this)
        FireworkRebarEntityHandler.register(this)
        InteractRebarEntityHandler.register(this)
        ItemRebarEntityHandler.register(this)
        LeashRebarEntityHandler.register(this)
        MountRebarEntityHandler.register(this)
        MoveRebarEntityHandler.register(this)
        PassengerRebarEntityHandler.register(this)
        PathfindRebarEntityHandler.register(this)
        PiglinRebarEntityHandler.register(this)
        ProjectileRebarEntityHandler.register(this)
        ResurrectRebarEntityHandler.register(this)
        SlimeRebarEntityHandler.register(this)
        SpellcasterRebarEntityHandler.register(this)
        TameRebarEntityHandler.register(this)
        TargetEntityRebarEntityHandler.register(this)
        TurtleRebarEntityHandler.register(this)
        UnloadRebarEntityHandler.register(this)
        VillagerRebarEntityHandler.register(this)
        WitchRebarEntityHandler.register(this)
        ZombifiedPiglinRebarEntityHandler.register(this)

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

    override val languages: Set<Locale> = setOf(
        Locale.ENGLISH,
        Locale.of("enws")
    )
}

private fun addDefaultPermission(permission: String) {
    Bukkit.getPluginManager().addPermission(Permission(permission, PermissionDefault.TRUE))
}