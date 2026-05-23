package io.github.pylonmc.rebar.test.test.block;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.gametest.GameTestConfig;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.GameTest;
import io.github.pylonmc.rebar.test.block.TestBlocks;
import org.bukkit.NamespacedKey;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockStorageAddTest extends GameTest {

    public BlockStorageAddTest() {
        super(new GameTestConfig.Builder(new NamespacedKey(RebarTest.instance(), "block_storage_add_test"))
                .size(1)
                .setUp((test) -> {
                    BlockStorage.placeBlock(test.location(), TestBlocks.SIMPLE_BLOCK_KEY);

                    RebarBlock rebarBlock = BlockStorage.get(test.location());

                    assertThat(rebarBlock)
                            .isNotNull()
                            .isInstanceOf(RebarBlock.class);

                    assertThat(BlockStorage.getAs(RebarBlock.class, test.location()))
                            .isNotNull();

                    BlockStorage.breakBlock(test.location());
                })
                .build());
    }
}
