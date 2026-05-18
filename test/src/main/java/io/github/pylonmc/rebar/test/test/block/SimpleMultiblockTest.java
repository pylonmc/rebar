package io.github.pylonmc.rebar.test.test.block;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.block.TestBlocks;
import io.github.pylonmc.rebar.test.block.TestRebarSimpleMultiblock;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;

import static org.assertj.core.api.Assertions.assertThat;


public class SimpleMultiblockTest extends AsyncTest {

    public static void assertMultiblockFormed(Location multiblockLocation, boolean formed) {
        assertThat(BlockStorage.get(multiblockLocation))
                .isInstanceOfSatisfying(TestRebarSimpleMultiblock.class, block ->
                        assertThat(block.isFormedAndFullyLoaded()).isEqualTo(formed)
                );
    }

    @Override
    protected void test() {
        Chunk chunk = TestUtil.getRandomChunk(false).join();

        Location multiblockLocation = chunk.getBlock(5, 100, 5).getLocation();
        Location component1Location = multiblockLocation.clone().add(1, 1, 4);
        Location component2Location = multiblockLocation.clone().add(2, -1, 0);

        TestUtil.runSync(() -> BlockStorage.placeBlock(multiblockLocation, TestRebarSimpleMultiblock.KEY)).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, false);

        TestUtil.runSync(() -> BlockStorage.placeBlock(component1Location, TestBlocks.SIMPLE_BLOCK_KEY)).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, false);

        TestUtil.runSync(() -> BlockStorage.placeBlock(component2Location, TestBlocks.SIMPLE_BLOCK_KEY)).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, true);

        TestUtil.runSync(() -> BlockStorage.breakBlock(component2Location)).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, false);

        TestUtil.unloadChunk(chunk).join();
        TestUtil.loadChunk(chunk).join();

        TestUtil.runSync(() -> BlockStorage.placeBlock(component2Location, TestBlocks.SIMPLE_BLOCK_KEY)).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, true);

        TestUtil.runSync(() -> {
            BlockStorage.breakBlock(multiblockLocation);
            BlockStorage.placeBlock(multiblockLocation, TestRebarSimpleMultiblock.KEY);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, true);

        TestUtil.unloadChunk(chunk).join();
    }
}
