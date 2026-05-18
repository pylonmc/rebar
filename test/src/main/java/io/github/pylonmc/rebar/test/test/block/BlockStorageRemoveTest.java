package io.github.pylonmc.rebar.test.test.block;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.gametest.GameTestConfig;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.GameTest;
import io.github.pylonmc.rebar.test.block.TestBlocks;
import org.bukkit.NamespacedKey;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockStorageRemoveTest extends GameTest {

    public BlockStorageRemoveTest() {
        super(new GameTestConfig.Builder(new NamespacedKey(RebarTest.instance(), "block_storage_remove_test"))
                .size(1)
                .setUp((test) -> {
                    BlockStorage.placeBlock(test.location(), TestBlocks.SIMPLE_BLOCK_KEY);
                    assertThat(BlockStorage.get(test.location()))
                            .isNotNull();

                    BlockStorage.breakBlock(test.location());
                    assertThat(BlockStorage.get(test.location()))
                            .isNull();
                })
                .build());
    }
}
