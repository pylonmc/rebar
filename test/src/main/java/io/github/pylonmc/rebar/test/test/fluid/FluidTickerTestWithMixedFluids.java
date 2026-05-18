package io.github.pylonmc.rebar.test.test.fluid;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.fluid.FluidManager;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.block.fluid.FluidConnector;
import io.github.pylonmc.rebar.test.block.fluid.FluidConsumer;
import io.github.pylonmc.rebar.test.block.fluid.FluidProducer;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.block.Block;


public class FluidTickerTestWithMixedFluids extends AsyncTest {

    @Override
    protected void test() {
        Chunk chunk = TestUtil.getRandomChunk(false).join();

        Block producerBlock1 = chunk.getBlock(2, 64, 4);
        FluidProducer producer1 = (FluidProducer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(producerBlock1, FluidProducer.WATER_PRODUCER_KEY)
        ).join();

        Block producerBlock2 = chunk.getBlock(2, 64, 6);
        FluidProducer producer2 = (FluidProducer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(producerBlock2, FluidProducer.LAVA_PRODUCER_KEY)
        ).join();

        Block connectorBlock = chunk.getBlock(4, 64, 5);
        FluidConnector connector = (FluidConnector) TestUtil.runSync(
                () -> BlockStorage.placeBlock(connectorBlock, FluidConnector.KEY)
        ).join();

        Block consumerBlock1 = chunk.getBlock(6, 64, 4);
        FluidConsumer consumer1 = (FluidConsumer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(consumerBlock1, FluidConsumer.WATER_CONSUMER_KEY)
        ).join();

        Block consumerBlock2 = chunk.getBlock(6, 64, 6);
        FluidConsumer consumer2 = (FluidConsumer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(consumerBlock2, FluidConsumer.LAVA_CONSUMER_KEY)
        ).join();

        TestUtil.runSync(() -> {
            FluidManager.connect(producer1.getPoint(), connector.getPoint());
            FluidManager.connect(producer2.getPoint(), connector.getPoint());
            FluidManager.connect(connector.getPoint(), consumer1.getPoint());
            FluidManager.connect(connector.getPoint(), consumer2.getPoint());
        }).join();

        TestUtil.waitUntil(() -> consumer1.getAmount() == 100 && consumer2.getAmount() == 100).join();

        TestUtil.unloadChunk(chunk).join();
    }
}
