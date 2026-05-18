package io.github.pylonmc.rebar.test.test.block;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.RebarBlockSchema;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.block.TestBlocks;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockStorageFilledChunkTest extends AsyncTest {

    @Override
    public void test() {
        Chunk chunk = TestUtil.getRandomChunk(false).join();

        List<Block> blocks = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    blocks.add(chunk.getBlock(x, y, z));
                }
            }
        }

        TestUtil.runSync(() -> {
            for (Block block : blocks) {
                BlockStorage.placeBlock(block, TestBlocks.SIMPLE_BLOCK_KEY);
            }
        }).join();
        TestUtil.unloadChunk(chunk).join();

        TestUtil.loadChunk(chunk).join();
        for (Block block : blocks) {
            RebarBlock rebarBlock = BlockStorage.get(block);

            assertThat(rebarBlock)
                    .isNotNull();

            assertThat(rebarBlock.getSchema())
                    .extracting(RebarBlockSchema::getKey)
                    .isEqualTo(TestBlocks.SIMPLE_BLOCK_KEY);
        }
        TestUtil.unloadChunk(chunk).join();
    }
}
