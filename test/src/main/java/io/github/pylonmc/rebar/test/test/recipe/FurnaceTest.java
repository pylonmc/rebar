package io.github.pylonmc.rebar.test.test.recipe;

import io.github.pylonmc.rebar.gametest.GameTestConfig;
import io.github.pylonmc.rebar.recipe.RecipeType;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.GameTest;
import io.github.pylonmc.rebar.test.item.TestItems;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public class FurnaceTest extends GameTest {

    public FurnaceTest() {
        super(new GameTestConfig.Builder(RebarTest.key("furnace_test"))
                .size(0)
                .setUp(test -> {
                    ItemStack stickyStick = TestItems.STICKY_STICK_STACK;
                    ItemStack diamond = ItemStack.of(Material.DIAMOND);
                    RecipeType.VANILLA_FURNACE.addRecipe(new FurnaceRecipe(
                            RebarTest.key("sticky_stick_furnace"),
                            diamond,
                            new RecipeChoice.ExactChoice(stickyStick),
                            0.1f,
                            10
                    ));

                    Block furnace = test.position().getBlock();
                    furnace.setType(Material.FURNACE);
                    Furnace state = (Furnace) furnace.getState();
                    FurnaceInventory inventory = state.getInventory();
                    inventory.setFuel(ItemStack.of(Material.STICK));
                    inventory.setSmelting(stickyStick);

                    test.succeedWhen(() -> {
                        FurnaceInventory inv = ((Furnace) furnace.getState()).getInventory();
                        ItemStack result = inv.getResult();
                        return diamond.equals(result);
                    });
                })
                .build()
        );
    }
}
