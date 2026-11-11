package io.github.pylonmc.rebar.test.test.item;

import io.github.pylonmc.rebar.logistics.slot.BrewingStandFuelLogisticSlot;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.SyncTest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import static org.assertj.core.api.Assertions.assertThat;

public class BrewingStandFuelTest extends SyncTest {

    @Override
    protected void test() {
        var inventory = Bukkit.createInventory(null, InventoryType.BREWING);
        var slot = new BrewingStandFuelLogisticSlot(RebarTest.testWorld.getBlockAt(0, 0, 0), inventory, 4);

        assertThat(slot.getMaxAmount(ItemStack.of(Material.BLAZE_POWDER))).isEqualTo(64L);
        assertThat(slot.getMaxAmount(ItemStack.of(Material.STONE))).isZero();
        assertThat(slot.getMaxAmount(ItemStack.of(Material.AIR))).isZero();

        var customFuel = Bukkit.getItemFactory().createItemStack(
                "minecraft:stone[minecraft:brewing_fuel={uses:20,speed_multiplier:1.0}]");
        var original = customFuel.clone();
        assertThat(slot.getMaxAmount(customFuel)).isEqualTo(customFuel.getMaxStackSize());
        assertThat(customFuel).isEqualTo(original);

        var removedFuel = Bukkit.getItemFactory().createItemStack(
                "minecraft:blaze_powder[!minecraft:brewing_fuel]");
        assertThat(slot.getMaxAmount(removedFuel)).isZero();
    }
}
