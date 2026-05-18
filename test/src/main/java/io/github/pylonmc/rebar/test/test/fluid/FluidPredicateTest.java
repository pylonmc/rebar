package io.github.pylonmc.rebar.test.test.fluid;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.fluid.FluidManager;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.block.fluid.FluidConsumer;
import io.github.pylonmc.rebar.test.block.fluid.FluidProducer;
import io.github.pylonmc.rebar.test.fluid.LavaTag;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import static org.assertj.core.api.Assertions.assertThat;


public class FluidPredicateTest extends AsyncTest {

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
            FluidManager.setFluidPredicate(waterProducer.getPoint().getSegment(), fluid -> fluid.hasTag(LavaTag.class));

            FluidManager.connect(lavaProducer.getPoint(), lavaConsumer.getPoint());
            FluidManager.setFluidPredicate(lavaProducer.getPoint().getSegment(), fluid -> fluid.hasTag(LavaTag.class));
            // also check that the predicate is preserved across connects/disconnects
            FluidManager.disconnect(lavaProducer.getPoint(), lavaConsumer.getPoint());
            FluidManager.connect(lavaProducer.getPoint(), lavaConsumer.getPoint());
        }).join();

        TestUtil.sleepTicks(5).join();

        assertThat(waterConsumer.getAmount())
                .isEqualTo(0);

        assertThat(lavaConsumer.getAmount())
                .isNotEqualTo(0);

        TestUtil.unloadChunk(chunk).join();
    }
}
