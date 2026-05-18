package io.github.pylonmc.rebar.test.test.block;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.block.TestBlocks;
import io.github.pylonmc.rebar.test.block.TestRebarSimpleMultiblock;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;

import static io.github.pylonmc.rebar.test.test.block.SimpleMultiblockTest.assertMultiblockFormed;


public class SimpleMultiblockRotatedTest extends AsyncTest {

    @Override
    protected void test() {
        Chunk chunk = TestUtil.getRandomChunk(false).join();

        Location multiblockLocation = chunk.getBlock(5, 100, 5).getLocation();

        TestUtil.runSync(() -> BlockStorage.placeBlock(multiblockLocation, TestRebarSimpleMultiblock.KEY)).join();

        // 0 degrees
        Location component1Location0 = multiblockLocation.clone().add(1, 1, 4);
        Location component2Location0 = multiblockLocation.clone().add(2, -1, 0);
        TestUtil.runSync(() -> {
            BlockStorage.placeBlock(component1Location0, TestBlocks.SIMPLE_BLOCK_KEY);
            BlockStorage.placeBlock(component2Location0, TestBlocks.SIMPLE_BLOCK_KEY);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, true);

        TestUtil.runSync(() -> {
            BlockStorage.breakBlock(component1Location0);
            BlockStorage.breakBlock(component2Location0);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, false);

        // 90 degrees (anticlockwise)
        Location component1Location1 = multiblockLocation.clone().add(-4, 1, 1);
        Location component2Location1 = multiblockLocation.clone().add(0, -1, 2);
        TestUtil.runSync(() -> {
            BlockStorage.placeBlock(component1Location1, TestBlocks.SIMPLE_BLOCK_KEY);
            BlockStorage.placeBlock(component2Location1, TestBlocks.SIMPLE_BLOCK_KEY);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, true);

        TestUtil.runSync(() -> {
            BlockStorage.breakBlock(component1Location1);
            BlockStorage.breakBlock(component2Location1);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, false);

        // 180 degrees
        Location component1Location2 = multiblockLocation.clone().add(-1, 1, -4);
        Location component2Location2 = multiblockLocation.clone().add(-2, -1, 0);
        TestUtil.runSync(() -> {
            BlockStorage.placeBlock(component1Location2, TestBlocks.SIMPLE_BLOCK_KEY);
            BlockStorage.placeBlock(component2Location2, TestBlocks.SIMPLE_BLOCK_KEY);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, true);

        TestUtil.runSync(() -> {
            BlockStorage.breakBlock(component1Location2);
            BlockStorage.breakBlock(component2Location2);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, false);

        // 270 degrees (anticlockwise)
        Location component1Location3 = multiblockLocation.clone().add(4, 1, -1);
        Location component2Location3 = multiblockLocation.clone().add(0, -1, -2);
        TestUtil.runSync(() -> {
            BlockStorage.placeBlock(component1Location3, TestBlocks.SIMPLE_BLOCK_KEY);
            BlockStorage.placeBlock(component2Location3, TestBlocks.SIMPLE_BLOCK_KEY);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, true);

        TestUtil.runSync(() -> {
            BlockStorage.breakBlock(component1Location3);
            BlockStorage.breakBlock(component2Location3);
        }).join();
        TestUtil.sleepTicks(2).join();
        assertMultiblockFormed(multiblockLocation, false);

        TestUtil.unloadChunk(chunk).join();
    }
}
