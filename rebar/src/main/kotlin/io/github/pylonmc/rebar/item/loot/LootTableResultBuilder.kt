package io.github.pylonmc.rebar.item.loot

import io.github.pylonmc.rebar.nms.NmsAccessor
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.NamespacedKey.minecraft
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTable
import org.bukkit.loot.Lootable
import org.bukkit.util.Vector
import org.joml.Vector3d

class LootTableResultBuilder {
    var thisEntity: Entity? = null
        private set
    var interactingEntity: Entity? = null
        private set
    var targetEntity: Entity? = null
        private set
    var lastDamagePlayer: Player? = null
        private set
    var damageSource: DamageSource? = null
        private set
    var attackingEntity: Entity? = null
        private set
    var directAttackingEntity: Entity? = null
        private set
    var origin: Vector3d? = null
        private set
    var blockData: BlockData? = null // In NMS -> BlockState
        private set
    var blockState: BlockState? = null // In NMS -> BlockEntity
        private set
    var tool: ItemStack? = null
        private set
    var explosionRadius: Float? = null
        private set
    var enchantmentLevel: Int? = null
        private set
    var enchantmentActive: Boolean? = null
        private set
    var additionalCostComponentAllowed: Unit? = null
        private set
    
    fun setThisEntity(entity: Entity?) = apply { this.thisEntity = entity }
    fun setInteractingEntity(entity: Entity?) = apply { this.interactingEntity = entity }
    fun setTargetEntity(entity: Entity?) = apply { this.targetEntity = entity }
    fun setLastDamagePlayer(player: Player?) = apply { this.lastDamagePlayer = player }
    fun setDamageSource(source: DamageSource?) = apply { this.damageSource = source }
    fun setAttackingEntity(entity: Entity?) = apply { this.attackingEntity = entity }
    fun setDirectAttackingEntity(entity: Entity?) = apply { this.directAttackingEntity = entity }
    fun setOrigin(origin: Vector3d?) = apply { this.origin = origin }
    fun setOrigin(origin: Vector?) = setOrigin(origin?.toVector3d())
    fun setOrigin(origin: Location?) = setOrigin(origin?.toVector())
    fun setBlockData(blockData: BlockData?) = apply { this.blockData = blockData }
    fun setBlockState(blockState: BlockState?) = apply { this.blockState = blockState }
    fun setTool(tool: ItemStack?) = apply { this.tool = tool }
    fun setExplosionRadius(radius: Float?) = apply { this.explosionRadius = radius }
    fun setEnchantmentLevel(level: Int?) = apply { this.enchantmentLevel = level }
    fun setEnchantmentActive(active: Boolean?) = apply { this.enchantmentActive = active }
    fun setAdditionalCostComponentAllowed(allowed: Unit?) = apply { this.additionalCostComponentAllowed = allowed }
    
    fun getRandomItems(world: World, contextSet: NamespacedKey, lootable: Lootable): Collection<ItemStack>? {
        val lootTable = lootable.lootTable ?: return null
        return getRandomItems(world, contextSet, lootTable, lootable.seed)
    }

    @JvmOverloads
    fun getRandomItems(world: World, contextSet: NamespacedKey, lootTable: LootTable, optionalLootTableSeed: Long? = null): Collection<ItemStack> {
        return NmsAccessor.instance.getRandomItems(world, contextSet, lootTable, optionalLootTableSeed, this)
    }

    companion object {
        @JvmField val EMPTY = minecraft("empty")
        @JvmField val CHEST = minecraft("chest")
        @JvmField val COMMAND = minecraft("command")
        @JvmField val SELECTOR = minecraft("selector")
        @JvmField val VILLAGER_TRADE = minecraft("villager_trade")
        @JvmField val FISHING = minecraft("fishing")
        @JvmField val ENTITY = minecraft("entity")
        @JvmField val EQUIPMENT = minecraft("equipment")
        @JvmField val ARCHAEOLOGY = minecraft("archaeology")
        @JvmField val GIFT = minecraft("gift")
        @JvmField val PIGLIN_BARTER = minecraft("barter")
        @JvmField val VAULT = minecraft("vault")
        @JvmField val ADVANCEMENT_REWARD = minecraft("advancement_reward")
        @JvmField val ADVANCEMENT_ENTITY = minecraft("advancement_entity")
        @JvmField val ADVANCEMENT_LOCATION = minecraft("advancement_location")
        @JvmField val BLOCK_USE = minecraft("block_use")
        @JvmField val GENERIC = minecraft("generic")
        @JvmField val BLOCK = minecraft("block")
        @JvmField val SHEARING = minecraft("shearing")
        @JvmField val ENTITY_INTERACT = minecraft("entity_interact")
        @JvmField val BLOCK_INTERACT = minecraft("block_interact")
        @JvmField val ENCHANTED_DAMAGE = minecraft("enchanted_damage")
        @JvmField val ENCHANTED_ITEM = minecraft("enchanted_item")
        @JvmField val ENCHANTED_LOCATION = minecraft("enchanted_location")
        @JvmField val ENCHANTED_ENTITY = minecraft("enchanted_entity")
        @JvmField val HIT_BLOCK = minecraft("hit_block")

        @JvmStatic
        fun of(event: EntityDeathEvent): LootTableResultBuilder {
            return LootTableResultBuilder()
                .setThisEntity(event.entity)
                .setOrigin(event.entity.location)
                .setDamageSource(event.damageSource)
                .setAttackingEntity(event.damageSource.causingEntity)
                .setDirectAttackingEntity(event.damageSource.directEntity)
                .setLastDamagePlayer(event.damageSource.causingEntity as? Player)
        }
    }
}