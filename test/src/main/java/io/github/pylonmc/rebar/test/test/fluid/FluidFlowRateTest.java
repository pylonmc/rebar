package io.github.pylonmc.rebar.test.test.fluid;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.fluid.FluidManager;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.block.fluid.FluidConsumer;
import io.github.pylonmc.rebar.test.block.fluid.FluidProducer;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import static org.assertj.core.api.Assertions.assertThat;


public class FluidFlowRateTest extends AsyncTest {

    @Override
    protected void test() {
        Chunk chunk = TestUtil.getRandomChunk(false).join();

        Block waterProducerBlock = chunk.getBlock(2, 64, 5);
        FluidProducer waterProducer = (FluidProducer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(waterProducerBlock, FluidProducer.WATER_PRODUCER_KEY)
        ).join();

        Block waterConsumerBlock = chunk.getBlock(6, 64, 4);
        FluidConsumer waterConsumer = (FluidConsumer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(waterConsumerBlock, FluidConsumer.WATER_CONSUMER_KEY)
        ).join();

        Block lavaProducerBlock = chunk.getBlock(3, 64, 5);
        FluidProducer lavaProducer = (FluidProducer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(lavaProducerBlock, FluidProducer.LAVA_PRODUCER_KEY)
        ).join();

        Block lavaConsumerBlock = chunk.getBlock(7, 64, 4);
        FluidConsumer lavaConsumer = (FluidConsumer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(lavaConsumerBlock, FluidConsumer.LAVA_CONSUMER_KEY)
        ).join();

        TestUtil.runSync(() -> {
            FluidManager.connect(waterProducer.getPoint(), waterConsumer.getPoint());
            FluidManager.setFluidPerSecond(waterProducer.getPoint().getSegment(), 1);

            FluidManager.connect(lavaProducer.getPoint(), lavaConsumer.getPoint());
            FluidManager.setFluidPerSecond(lavaProducer.getPoint().getSegment(), 2);
        }).join();

        TestUtil.sleepTicks(5).join();

        assertThat(waterConsumer.getAmount() * 2)
                .isGreaterThan(lavaConsumer.getAmount() - 0.101)
                .isLessThan(lavaConsumer.getAmount() + 0.101);

        TestUtil.unloadChunk(chunk).join();
    }
}
