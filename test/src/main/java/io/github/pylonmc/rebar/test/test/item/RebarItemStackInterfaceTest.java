package io.github.pylonmc.rebar.test.test.item;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.gametest.GameTestConfig;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.GameTest;
import io.github.pylonmc.rebar.test.item.OminousBlazePower;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.BrewingStandFuelEvent;


public class RebarItemStackInterfaceTest extends GameTest {

    public RebarItemStackInterfaceTest() {
        super(new GameTestConfig.Builder(RebarTest.key("rebar_item_stack_interface_test"))
                .size(0)
                .timeoutTicks(100)
                .setUp((test) -> {
                    OminousBlazePower.handlerCalled = false;

                    test.succeedWhen(() -> OminousBlazePower.handlerCalled);

                    Block block = test.getWorld().getBlockAt(test.location());
                    block.setType(Material.BREWING_STAND);
                    Bukkit.getPluginManager().callEvent(new BrewingStandFuelEvent(block, OminousBlazePower.STACK, 1));
                })
                .build());
    }
}
