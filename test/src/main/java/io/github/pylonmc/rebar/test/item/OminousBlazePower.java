package io.github.pylonmc.rebar.test.item;

import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.base.RebarBrewingStandFuel;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public class OminousBlazePower extends RebarItem implements RebarBrewingStandFuel {

    public static final NamespacedKey KEY = RebarTest.key("ominous_blaze_powder");
    public static final ItemStack STACK = ItemStackBuilder.rebar(Material.DIAMOND_SWORD, KEY)
            .name("<ff0000>OMINOUS BLAZE POWDER")
            .lore("<#ff0000>VERY SCARY")
            .lore("<#222222>OH NO")
            .build();
    public static boolean handlerCalled;

    public OminousBlazePower(@NotNull ItemStack stack) {
        super(stack);
    }

    @Override
    public void onUsedAsBrewingStandFuel(@NotNull BrewingStandFuelEvent event, @NotNull EventPriority priority) {
        event.setCancelled(true);
        handlerCalled = true;
    }
}