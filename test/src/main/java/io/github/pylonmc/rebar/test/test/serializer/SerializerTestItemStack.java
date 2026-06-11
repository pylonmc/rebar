package io.github.pylonmc.rebar.test.test.serializer;

import io.github.pylonmc.rebar.datatypes.RebarSerializers;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public class SerializerTestItemStack extends SerializerTest<ItemStack> {
    private static @NotNull ItemStack getStack() {
        ItemStack value = ItemStack.of(Material.ACACIA_BOAT);
        // Just random properties to test
        value.editMeta((meta) -> {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.setUnbreakable(true);
            meta.setCustomModelData(2);
            meta.setGlider(true);
        });
        return value;
    }

    public SerializerTestItemStack() {
        super(getStack(), RebarSerializers.ITEM_STACK);
    }
}
