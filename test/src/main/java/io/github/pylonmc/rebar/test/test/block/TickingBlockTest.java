package io.github.pylonmc.rebar.test.test.block;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.config.RebarConfig;
import io.github.pylonmc.rebar.gametest.GameTestConfig;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.GameTest;
import io.github.pylonmc.rebar.test.block.TickingBlock;


public class TickingBlockTest extends GameTest {

    public TickingBlockTest() {
        super(new GameTestConfig.Builder(RebarTest.key("ticking_block"))
                .size(1)
                .setUp((test) -> {
                    BlockStorage.placeBlock(test.location(), TickingBlock.KEY);

                    test.succeedWhen(() -> BlockStorage.getAs(TickingBlock.class, test.location()).ticks >= 5);
                })
                .cleanup(test -> BlockStorage.breakBlock(test.location()))
                .build());
    }
}
