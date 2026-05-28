@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.async.BukkitMainThreadDispatcher
import io.github.pylonmc.rebar.async.ChunkScope
import io.github.pylonmc.rebar.async.PlayerScope
import io.github.pylonmc.rebar.block.*
import io.github.pylonmc.rebar.block.base.*
import io.github.pylonmc.rebar.block.base.handler.FallingRebarBlockHandler.RebarFallingBlockEntity
import io.github.pylonmc.rebar.block.base.handler.BeaconRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.BellRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.BrewingStandRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.CampfireRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.CargoRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.CauldronRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.ComposterRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.CopperRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.CrafterRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.DispenserRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.EnchantingTableRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.EntityChangeRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.FallingRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.FireRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.FlowerPotRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.FurnaceRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.GrowableRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.HopperRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.InteractableRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.JobRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.JumpRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.LeafRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.LecternRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.LootDispenserRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.NoteRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.PistonRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.RedstoneRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.ShearableRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.SignRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.SneakRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.SpongeRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.TNTRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.TargetRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.UnloadRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.VanillaInventoryRebarBlockHandler
import io.github.pylonmc.rebar.block.base.handler.VaultRebarBlockHandler
import io.github.pylonmc.rebar.command.ROOT_COMMAND
import io.github.pylonmc.rebar.command.ROOT_COMMAND_RE_ALIAS
import io.github.pylonmc.rebar.config.Config
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
import io.github.pylonmc.rebar.entity.base.*
import io.github.pylonmc.rebar.entity.base.handler.BatRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.BreakDoorRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.BreedableRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.CombustibleRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.CreeperRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.DamageRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.DeathRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.DragonFireballRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.DyeableRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.EnderDragonRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.EndermanRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.ExperienceOrbRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.ExplosiveRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.FireworkRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.InteractableRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.ItemRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.LeashableRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.MountableRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.MoveRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.PassengerRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.PathfindRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.PiglinRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.ProjectileRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.ResurrectRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.SlimeRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.SpellcasterRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.TameableRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.TurtleRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.VillagerRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.WitchRebarEntityHandler
import io.github.pylonmc.rebar.entity.base.handler.ZombifiedPiglinRebarEntityHandler
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
import io.github.pylonmc.rebar.item.base.handler.AnvilUseRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.ArrowRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.BlockBreakRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.BlockInteractRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.BottleRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.BowRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.BrewingStandFuelRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.BucketRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.ConsumableRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.DispenseRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.DropRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.DurabilityRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.EntityAttackRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.EntityInteractRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.FurnaceBurnRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.InteractableRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.JoinRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.LingeringPotionRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.PickupRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.ProjectileRebarItemHandler
import io.github.pylonmc.rebar.item.base.handler.SplashPotionRebarItemHandler
import io.github.pylonmc.rebar.util.mergeGlobalConfig
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
            logger.severe("Rebar may run fine, but you may encounter bugs ranging from mild to catastrophic")
            logger.severe("Please update your Rebar version accordingly")
            logger.severe("Please see https://github.com/pylonmc/rebar/releases for available Rebar versions")
            logger.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        }

        InvUI.getInstance().setPlugin(this)
        Languages.getInstance().enableServerSideTranslations(false) // we do our own

        saveDefaultConfig()
        // Add any keys that are missing from global config - saveDefaultConfig will not do anything if config already present
        mergeGlobalConfig(Rebar, "config.yml", "config.yml")

        val pm = Bukkit.getPluginManager()
        pm.registerEvents(RebarTranslator, this)
        pm.registerEvents(RebarAddon, this)

        RebarMetrics // initialize metrics by referencing it

        // Anything that listens for addon registration must be above this line
        registerWithRebar()
        pm.registerEvents(RebarItemListener, this)
        pm.registerEvents(BlockStorage, this)
        pm.registerEvents(MultiblockCache, this)
        pm.registerEvents(EntityStorage, this)
        pm.registerEvents(Research, this)
        pm.registerEvents(VirtualInventoryRebarBlock, this)
        pm.registerEvents(GuiRebarBlock, this)
        pm.registerEvents(EntityHolderRebarBlock, this)
        pm.registerEvents(SimpleRebarMultiblock, this)
        pm.registerEvents(ProcessorRebarBlock, this)
        pm.registerEvents(RecipeProcessorRebarBlock, this)
        pm.registerEvents(FluidBufferRebarBlock, this)
        pm.registerEvents(FluidTankRebarBlock, this)
        pm.registerEvents(RebarRecipeListener, this)
        pm.registerEvents(DirectionalRebarBlock, this)
        pm.registerEvents(FluidPipePlacementService, this)
        pm.registerEvents(TickingRebarBlock, this)
        pm.registerEvents(RebarGuide, this)
        pm.registerEvents(LogisticRebarBlock, this)
        pm.registerEvents(CargoRoutes, this)
        pm.registerEvents(CargoDuct, this)
        pm.registerEvents(RecipeCompletion, this)
        pm.registerEvents(PagedGuidePage, this)
        pm.registerEvents(TabbedGuidePage, this)
        pm.registerEvents(TickingRebarEntity, this)
        pm.registerEvents(ChunkScope, this)
        pm.registerEvents(PlayerScope, this)
        pm.registerEvents(CreativeActionTranslationHandler, this)
        ConfettiCreeperListener.register(this, pm)

        // Rebar Blocks
        pm.registerEvents(CargoRebarBlock, this)
        pm.registerEvents(FallingRebarBlockHandler, this)
        BlockListener.register(this, pm)
        BeaconRebarBlockHandler.register(this, pm)
        BellRebarBlockHandler.register(this, pm)
        TNTRebarBlockHandler.register(this, pm)
        NoteRebarBlockHandler.register(this, pm)
        CrafterRebarBlockHandler.register(this, pm)
        SpongeRebarBlockHandler.register(this, pm)
        FurnaceRebarBlockHandler.register(this, pm)
        CampfireRebarBlockHandler.register(this, pm)
        BrewingStandRebarBlockHandler.register(this, pm)
        DispenserRebarBlockHandler.register(this, pm)
        GrowableRebarBlockHandler.register(this, pm)
        CauldronRebarBlockHandler.register(this, pm)
        SignRebarBlockHandler.register(this, pm)
        VaultRebarBlockHandler.register(this, pm)
        LeafRebarBlockHandler.register(this, pm)
        TargetRebarBlockHandler.register(this, pm)
        ComposterRebarBlockHandler.register(this, pm)
        ShearableRebarBlockHandler.register(this, pm)
        LecternRebarBlockHandler.register(this, pm)
        PistonRebarBlockHandler.register(this, pm)
        EnchantingTableRebarBlockHandler.register(this, pm)
        RedstoneRebarBlockHandler.register(this, pm)
        InteractableRebarBlockHandler.register(this, pm)
        SneakRebarBlockHandler.register(this, pm)
        JobRebarBlockHandler.register(this, pm)
        JumpRebarBlockHandler.register(this, pm)
        UnloadRebarBlockHandler.register(this, pm)
        FlowerPotRebarBlockHandler.register(this, pm)
        VanillaInventoryRebarBlockHandler.register(this, pm)
        HopperRebarBlockHandler.register(this, pm)
        FireRebarBlockHandler.register(this, pm)
        CargoRebarBlockHandler.register(this, pm)
        CopperRebarBlockHandler.register(this, pm)
        EntityChangeRebarBlockHandler.register(this, pm)
        LootDispenserRebarBlockHandler.register(this, pm)

        // Rebar Items
        ArrowRebarItemHandler.register(this, pm)
        BlockInteractRebarItemHandler.register(this, pm)
        BottleRebarItemHandler.register(this, pm)
        BowRebarItemHandler.register(this, pm)
        BrewingStandFuelRebarItemHandler.register(this, pm)
        BucketRebarItemHandler.register(this, pm)
        ConsumableRebarItemHandler.register(this, pm)
        DispenseRebarItemHandler.register(this, pm)
        InteractableRebarItemHandler.register(this, pm)
        DurabilityRebarItemHandler.register(this, pm)
        EntityInteractRebarItemHandler.register(this, pm)
        JoinRebarItemHandler.register(this, pm)
        LingeringPotionRebarItemHandler.register(this, pm)
        ProjectileRebarItemHandler.register(this, pm)
        SplashPotionRebarItemHandler.register(this, pm)
        BlockBreakRebarItemHandler.register(this, pm)
        EntityAttackRebarItemHandler.register(this, pm)
        AnvilUseRebarItemHandler.register(this, pm)
        FurnaceBurnRebarItemHandler.register(this, pm)
        PickupRebarItemHandler.register(this, pm)
        DropRebarItemHandler.register(this, pm)

        // Rebar Entities
        EntityListener.register(this, pm)
        BatRebarEntityHandler.register(this, pm)
        BreedableRebarEntityHandler.register(this, pm)
        CombustibleRebarEntityHandler.register(this, pm)
        BreakDoorRebarEntityHandler.register(this, pm)
        CreeperRebarEntityHandler.register(this, pm)
        DamageRebarEntityHandler.register(this, pm)
        DeathRebarEntityHandler.register(this, pm)
        DragonFireballRebarEntityHandler.register(this, pm)
        DyeableRebarEntityHandler.register(this, pm)
        EnderDragonRebarEntityHandler.register(this, pm)
        EndermanRebarEntityHandler.register(this, pm)
        ExperienceOrbRebarEntityHandler.register(this, pm)
        ExplosiveRebarEntityHandler.register(this, pm)
        FireworkRebarEntityHandler.register(this, pm)
        InteractableRebarEntityHandler.register(this, pm)
        ItemRebarEntityHandler.register(this, pm)
        LeashableRebarEntityHandler.register(this, pm)
        MountableRebarEntityHandler.register(this, pm)
        PassengerRebarEntityHandler.register(this, pm)
        MoveRebarEntityHandler.register(this, pm)
        PathfindRebarEntityHandler.register(this, pm)
        PiglinRebarEntityHandler.register(this, pm)
        ProjectileRebarEntityHandler.register(this, pm)
        ResurrectRebarEntityHandler.register(this, pm)
        SlimeRebarEntityHandler.register(this, pm)
        SpellcasterRebarEntityHandler.register(this, pm)
        TameableRebarEntityHandler.register(this, pm)
        TurtleRebarEntityHandler.register(this, pm)
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

        RebarEntity.register<FallingBlock, RebarFallingBlockEntity>(FallingRebarBlockHandler.KEY)

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
        RebarConfigurableRecipesLoadedEvent().callEvent()
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