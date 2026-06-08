package io.github.pylonmc.rebar.test.test.recipe;

import io.github.pylonmc.rebar.recipe.RecipeType;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.SyncTest;
import io.github.pylonmc.rebar.test.item.TestItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class CraftingTest extends SyncTest {

    @Override
    protected void test() {
        ItemStack stickyStick = TestItems.STICKY_STICK_STACK;
        ItemStack diamond = ItemStack.of(Material.DIAMOND);
        ItemStack nothing = ItemStack.of(Material.AIR);
        ItemStack normalStick = ItemStack.of(Material.STICK);

        // Shaped
        {
            RecipeType.VANILLA_SHAPED.addRecipe(
                    new ShapedRecipe(RebarTest.key("sticky_stick_shaped"), diamond)
                            .shape(
                                    " s ",
                                    "sSs",
                                    " s "
                            )
                            .setIngredient('s', Material.STICK)
                            .setIngredient('S', stickyStick)
            );
            ItemStack[] crafting = {
                    nothing, normalStick, nothing,
                    normalStick, stickyStick, normalStick,
                    nothing, normalStick, nothing
            };
            assertThat(Bukkit.craftItem(crafting, RebarTest.testWorld))
                    .isEqualTo(diamond);
        }

        // Shapeless
        {
            RecipeType.VANILLA_SHAPELESS.addRecipe(
                    new ShapelessRecipe(RebarTest.key("sticky_stick_shapeless"), normalStick)
                            .addIngredient(Material.DIAMOND)
                            .addIngredient(stickyStick)
            );
            ItemStack[] crafting = new ItemStack[9];
            Arrays.fill(crafting, nothing);
            crafting[0] = stickyStick;
            crafting[1] = diamond;
            assertThat(Bukkit.craftItem(crafting, RebarTest.testWorld))
                    .isEqualTo(normalStick);
        }

        // With custom output
        {
            RecipeType.VANILLA_SHAPED.addRecipe(
                    new ShapedRecipe(RebarTest.key("sticky_stick_shaped_custom_output"), stickyStick)
                            .shape(
                                    " s ",
                                    "sDs",
                                    " s "
                            )
                            .setIngredient('s', Material.STICK)
                            .setIngredient('D', diamond)
            );
            ItemStack[] crafting = {
                    nothing, normalStick, nothing,
                    normalStick, diamond, normalStick,
                    nothing, normalStick, nothing
            };
            assertThat(Bukkit.craftItem(crafting, RebarTest.testWorld))
                    .isEqualTo(stickyStick);
        }
    }
}
