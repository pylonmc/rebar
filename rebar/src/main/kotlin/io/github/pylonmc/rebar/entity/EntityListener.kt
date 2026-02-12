package io.github.pylonmc.rebar.entity

import com.destroystokyo.paper.event.entity.*
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.base.RebarHopper
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.entity.base.*
import io.github.pylonmc.rebar.event.RebarEntityDeathEvent
import io.github.pylonmc.rebar.event.RebarEntityUnloadEvent
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener.logEventHandleErr
import io.github.pylonmc.rebar.item.base.RebarArrow
import io.github.pylonmc.rebar.item.base.RebarLingeringPotion
import io.github.pylonmc.rebar.item.base.RebarSplashPotion
import io.papermc.paper.event.entity.*
import io.papermc.paper.event.entity.EntityKnockbackEvent
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.minecart.HopperMinecart
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.util.UUID

internal object EntityListener : MultiListener {
    private val entityErrMap: MutableMap<UUID, Int> = mutableMapOf()

    @EventHandler(priority = EventPriority.MONITOR)
    private fun handle(event: PlayerInteractEntityEvent) {
        val rebarEntity = EntityStorage.get(event.rightClicked)
        if (rebarEntity is RebarInteractEntity) {
            try {
                rebarEntity.onInteract(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun handle(event: RebarEntityUnloadEvent) {
        if (event.rebarEntity is RebarUnloadEntity) {
            try {
                event.rebarEntity.onUnload(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, event.rebarEntity)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun handle(event: RebarEntityDeathEvent) {
        if (event.rebarEntity is RebarDeathEntity) {
            try {
                event.rebarEntity.onDeath(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, event.rebarEntity)
            }
        }
    }

    @UniversalHandler
    private fun handle(event: ProjectileHitEvent, priority: EventPriority) {
        if (event.entity is AbstractArrow) {
            val arrowItem = RebarItem.fromStack((event.entity as AbstractArrow).itemStack)
            if (arrowItem is RebarArrow) {
                try {
                    arrowItem.onArrowHit(event)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, arrowItem)
                }
            }
        }
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarProjectile) {
            try {
                rebarEntity.onHit(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun onInventoryPickup(event: InventoryPickupItemEvent) {
        val inv = event.inventory
        val holder = inv.holder
        if (holder is HopperMinecart) {
            val pyEntity = EntityStorage.get(holder.entity) as? RebarHopper ?: return
            pyEntity.onHopperPickUpItem(event, EventPriority.NORMAL)
        }
    }

    @UniversalHandler
    private fun handle(event: EntityDamageByEntityEvent, priority: EventPriority) {
        if (event.damager is AbstractArrow) {
            val arrowItem = RebarItem.fromStack((event.damager as AbstractArrow).itemStack)
            if (arrowItem is RebarArrow) {
                try {
                    arrowItem.onArrowDamage(event)
                } catch (e: Exception) {
                    logEventHandleErr(event, e, arrowItem)
                }
            }
        }
    }

    @UniversalHandler
    private fun handle(event: PotionSplashEvent, priority: EventPriority) {
        val rebarPotion = RebarItem.fromStack(event.potion.item)
        if (rebarPotion is RebarSplashPotion) {
            try {
                rebarPotion.onSplash(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarPotion)
            }
        }
    }

    @EventHandler
    private fun handle(event: LingeringPotionSplashEvent, priority: EventPriority) {
        val rebarPotion = RebarItem.fromStack(event.entity.item)
        if (rebarPotion is RebarLingeringPotion) {
            try {
                rebarPotion.onSplash(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarPotion)
            }
        }
    }

    @EventHandler
    private fun handle(event: CreeperIgniteEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarCreeper) {
            try {
                rebarEntity.onIgnite(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: CreeperPowerEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarCreeper) {
            try {
                rebarEntity.onPower(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EnderDragonChangePhaseEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarEnderDragon) {
            try {
                rebarEntity.onChangePhase(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EnderDragonFlameEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarEnderDragon) {
            try {
                rebarEntity.onFlame(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EnderDragonFireballHitEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarDragonFireball) {
            try {
                rebarEntity.onHit(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EnderDragonShootFireballEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarEnderDragon) {
            try {
                rebarEntity.onShootFireball(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: BatToggleSleepEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarBat) {
            try {
                rebarEntity.onToggleSleep(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EndermanAttackPlayerEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarEnderman) {
            try {
                rebarEntity.onAttackPlayer(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EndermanEscapeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarEnderman) {
            try {
                rebarEntity.onEscape(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityBreedEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarBreedable) {
            try {
                rebarEntity.onBreed(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityEnterLoveModeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarBreedable) {
            try {
                rebarEntity.onEnterLoveMode(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityBreakDoorEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarCop) {
            try {
                rebarEntity.kickDoor(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityCombustEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarCombustibleEntity) {
            try {
                rebarEntity.onCombust(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityDyeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarDyeable) {
            try {
                rebarEntity.onDye(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityPathfindEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarPathingEntity) {
            try {
                rebarEntity.onFindPath(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityTargetEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarPathingEntity) {
            try {
                rebarEntity.onTarget(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityMoveEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarMovingEntity) {
            try {
                rebarEntity.onMove(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityJumpEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarMovingEntity) {
            try {
                rebarEntity.onJump(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityKnockbackEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarMovingEntity) {
            try {
                rebarEntity.onKnockback(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityToggleSwimEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarMovingEntity) {
            try {
                rebarEntity.onToggleSwim(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityToggleGlideEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarMovingEntity) {
            try {
                rebarEntity.onToggleGlide(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityToggleSitEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarMovingEntity) {
            try {
                rebarEntity.onToggleSit(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityDamageEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarDamageableEntity) {
            try {
                rebarEntity.onDamage(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityRegainHealthEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarDamageableEntity) {
            try {
                rebarEntity.onRegainHealth(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityMountEvent, priority: EventPriority) {
        val mount = EntityStorage.get(event.mount)
        if (mount is RebarMountableEntity) {
            try {
                mount.onMount(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, mount)
            }
        }
        val mounter = EntityStorage.get(event.entity)
        if (mounter is RebarMountingEntity) {
            try {
                mounter.onMount(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, mounter)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityDismountEvent, priority: EventPriority) {
        val mount = EntityStorage.get(event.dismounted)
        if (mount is RebarMountableEntity) {
            try {
                mount.onDismount(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, mount)
            }
        }
        val dismounter = EntityStorage.get(event.entity)
        if (dismounter is RebarMountingEntity) {
            try {
                dismounter.onDismount(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, dismounter)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntitySpellCastEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarSpellcaster) {
            try {
                rebarEntity.onCastSpell(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityResurrectEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarResurrectable) {
            try {
                rebarEntity.onResurrect(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityTameEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarTameable) {
            try {
                rebarEntity.onTamed(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: TameableDeathMessageEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarTameable) {
            try {
                rebarEntity.onDeath(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: PlayerLeashEntityEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarLeashable) {
            try {
                rebarEntity.onLeash(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: EntityUnleashEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarLeashable) {
            try {
                rebarEntity.onUnleash(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: ItemDespawnEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarItemEntity) {
            try {
                rebarEntity.onDespawn(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: ItemMergeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarItemEntity) {
            try {
                rebarEntity.onMerge(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: PiglinBarterEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarPiglin) {
            try {
                rebarEntity.onBarter(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: PigZombieAngerEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarZombiePigman) {
            try {
                rebarEntity.onAnger(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: TurtleStartDiggingEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarTurtle) {
            try {
                rebarEntity.onStartDigging(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: TurtleGoHomeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarTurtle) {
            try {
                rebarEntity.onGoHome(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: TurtleLayEggEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarTurtle) {
            try {
                rebarEntity.onLayEgg(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: VillagerAcquireTradeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarVillager) {
            try {
                rebarEntity.onAcquireTrade(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: VillagerCareerChangeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarVillager) {
            try {
                rebarEntity.onCareerChange(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: VillagerReplenishTradeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarVillager) {
            try {
                rebarEntity.onReplenishTrade(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: WitchConsumePotionEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarWitch) {
            try {
                rebarEntity.onConsumePotion(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: WitchReadyPotionEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarWitch) {
            try {
                rebarEntity.onReadyPotion(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: WitchThrowPotionEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarWitch) {
            try {
                rebarEntity.onThrowPotion(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: SlimeSwimEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarSlime) {
            try {
                rebarEntity.onSwim(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: SlimeSplitEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarSlime) {
            try {
                rebarEntity.onSplit(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: SlimeWanderEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarSlime) {
            try {
                rebarEntity.onWander(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: SlimePathfindEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarSlime) {
            try {
                rebarEntity.onPathfind(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: SlimeChangeDirectionEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarSlime) {
            try {
                rebarEntity.onChangeDirection(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: SlimeTargetLivingEntityEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarSlime) {
            try {
                rebarEntity.onTarget(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: FireworkExplodeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarFirework) {
            rebarEntity.onExplode(event)
        }
    }

    @EventHandler
    private fun handle(event: ExplosionPrimeEvent, priority: EventPriority) {
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity is RebarExplosiveEntity) {
            try {
                rebarEntity.onPrime(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, rebarEntity)
            }
        }
    }

    @EventHandler
    private fun handle(event: ExperienceOrbMergeEvent, priority: EventPriority) {
        val source = EntityStorage.get(event.mergeSource)
        if (source is RebarExperienceOrb) {
            try {
                source.onMergeOrb(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, source)
            }
        }
        val target = EntityStorage.get(event.mergeTarget)
        if (target is RebarExperienceOrb) {
            try {
                target.onAbsorbedByOrb(event)
            } catch (e: Exception) {
                logEventHandleErr(event, e, target)
            }
        }
    }

    @JvmSynthetic
    internal fun logEventHandleErr(event: Event, e: Exception, entity: RebarEntity<*>) {
        Rebar.logger.severe("Error when handling entity(${entity.key}, ${entity.uuid}, ${entity.entity.location}) event handler ${event.javaClass.simpleName}: ${e.localizedMessage}")
        e.printStackTrace()
        entityErrMap[entity.uuid] = entityErrMap[entity.uuid]?.plus(1) ?: 1
        if (entityErrMap[entity.uuid]!! > RebarConfig.ALLOWED_ENTITY_ERRORS) {
            entity.entity.remove()
        }
    }

    @JvmSynthetic
    internal fun logEventHandleErrTicking(e: Exception, entity: RebarEntity<*>) {
        Rebar.logger.severe("Error when handling ticking entity(${entity.key}, ${entity.uuid}, ${entity.entity.location}): ${e.localizedMessage}")
        e.printStackTrace()
        entityErrMap[entity.uuid] = entityErrMap[entity.uuid]?.plus(1) ?: 1
        if (entityErrMap[entity.uuid]!! > RebarConfig.ALLOWED_ENTITY_ERRORS) {
            entity.entity.remove()
        }
    }

    @EventHandler
    private fun onEntityUnload(event: RebarEntityUnloadEvent) {
        entityErrMap.remove(event.rebarEntity.uuid)
    }
}