package io.github.pylonmc.rebar.test.test.block;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.PhantomBlock;
import io.github.pylonmc.rebar.config.RebarConfig;
import io.github.pylonmc.rebar.gametest.GameTestConfig;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.GameTest;
import io.github.pylonmc.rebar.test.block.BlockEventError;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.block.BellRingEvent;

public class BlockEventErrorTest extends GameTest {
    public BlockEventErrorTest(){
        super(new GameTestConfig.Builder(new NamespacedKey(RebarTest.instance(), "block_event_error_test"))
                .size(1)
                .setUp(test -> {
                    RebarConfig.FULL_ERROR_STACK_TRACES = false;
                    Block block = BlockStorage.placeBlock(test.location(), BlockEventError.KEY).getBlock();
                    Entity theRinger = test.location().getWorld().spawn(test.location().clone().add(1, 0, 0), Skeleton.class);
                    for(int i = 0; i < RebarConfig.ALLOWED_BLOCK_ERRORS + 1; i++){
                        new BellRingEvent(block, BlockFace.EAST, theRinger).callEvent();
                    }
                    test.succeedWhen(() -> BlockStorage.get(block) instanceof PhantomBlock);
                })
                .cleanup(test -> {
                    BlockStorage.breakBlock(test.location());
                    RebarConfig.FULL_ERROR_STACK_TRACES = true;
                })
                .build()
        );
    }
}
