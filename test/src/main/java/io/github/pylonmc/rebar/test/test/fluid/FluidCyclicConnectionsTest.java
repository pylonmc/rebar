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

import static org.assertj.core.api.Assertions.assertThat;


public class FluidCyclicConnectionsTest extends AsyncTest {

    @Override
    protected void test() {
        Chunk chunk = TestUtil.getRandomChunk(false).join();

        Block producerBlock = chunk.getBlock(2, 64, 5);
        FluidProducer producer = (FluidProducer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(producerBlock, FluidProducer.WATER_PRODUCER_KEY)
        ).join();
        assertThat(producer)
                .isNotNull();

        Block connectorBlock = chunk.getBlock(4, 64, 5);
        FluidConnector connector = (FluidConnector) TestUtil.runSync(
                () -> BlockStorage.placeBlock(connectorBlock, FluidConnector.KEY)
        ).join();
        assertThat(connector)
                .isNotNull();

        Block consumerBlock = chunk.getBlock(6, 64, 5);
        FluidConsumer consumer = (FluidConsumer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(consumerBlock, FluidConsumer.WATER_CONSUMER_KEY)
        ).join();
        assertThat(consumer)
                .isNotNull();

        // Create loop
        TestUtil.runSync(() -> {
            FluidManager.connect(consumer.getPoint(), producer.getPoint());
            FluidManager.connect(producer.getPoint(), connector.getPoint());
            FluidManager.connect(connector.getPoint(), consumer.getPoint());
        }).join();
        assertThat(consumer.getPoint().getSegment())
                .isEqualTo(producer.getPoint().getSegment())
                .isEqualTo(connector.getPoint().getSegment());

        // Disconnect one link; all should still have the same segment because they're connected in other ways still
        TestUtil.runSync(() -> FluidManager.disconnect(consumer.getPoint(), producer.getPoint())).join();
        assertThat(consumer.getPoint().getSegment())
                .isEqualTo(producer.getPoint().getSegment())
                .isEqualTo(connector.getPoint().getSegment());


        // Disconnect other links; all should now have distinct segments
        TestUtil.runSync(() -> {
            FluidManager.disconnect(producer.getPoint(), connector.getPoint());
            FluidManager.disconnect(connector.getPoint(), consumer.getPoint());
        }).join();
        assertThat(consumer.getPoint().getSegment())
                .isNotEqualTo(producer.getPoint().getSegment())
                .isNotEqualTo(connector.getPoint().getSegment());

        TestUtil.unloadChunk(chunk).join();
    }
}
