package io.github.pylonmc.rebar.test.item;

import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.block.TestBlocks;
import io.github.pylonmc.rebar.test.block.TestRebarSimpleMultiblock;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public final class TestItems {

    private TestItems() {}

    public static final NamespacedKey STICKY_STICK_KEY = RebarTest.key("sticky_stick");
    public static final ItemStack STICKY_STICK_STACK = ItemStackBuilder.rebar(Material.STICK, STICKY_STICK_KEY)
            .set(DataComponentTypes.ITEM_NAME, Component.text("Sticky Stick"))
            .build();

    public static void register() {
        RebarItem.register(RebarItem.class, STICKY_STICK_STACK);
        RebarItem.register(OminousBlazePower.class, OminousBlazePower.STACK);
        RebarItem.register(RebarItem.class, ItemStackBuilder.rebar(Material.AMETHYST_BLOCK, TestBlocks.SIMPLE_BLOCK_KEY).build());
        RebarItem.register(RebarItem.class, ItemStackBuilder.rebar(Material.AMETHYST_BLOCK, TestRebarSimpleMultiblock.KEY).build());
    }
}
