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


public class FluidPartialReloadTest extends AsyncTest {

    @Override
    protected void test() {
        Chunk producerChunk = TestUtil.getRandomChunk(false).join();
        Chunk connectorChunk = TestUtil.getRandomChunk(false).join();
        Chunk consumerChunk = TestUtil.getRandomChunk(false).join();

        Block consumerBlock = consumerChunk.getBlock(6, 64, 5);
        FluidConsumer consumer = (FluidConsumer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(consumerBlock, FluidConsumer.WATER_CONSUMER_KEY)
        ).join();

        Block connectorBlock = connectorChunk.getBlock(8, 64, 3);
        FluidConnector connector = (FluidConnector) TestUtil.runSync(
                () -> BlockStorage.placeBlock(connectorBlock, FluidConnector.KEY)
        ).join();

        Block producerBlock = producerChunk.getBlock(2, 64, 5);
        FluidProducer producer = (FluidProducer) TestUtil.runSync(
                () -> BlockStorage.placeBlock(producerBlock, FluidProducer.WATER_PRODUCER_KEY)
        ).join();

        TestUtil.runSync(() -> {
            FluidManager.connect(consumer.getPoint(), connector.getPoint());
            FluidManager.connect(producer.getPoint(), connector.getPoint());
        }).join();

        // All should initially have the same segment
        assertThat(consumer.getPoint().getSegment())
                .isEqualTo(connector.getPoint().getSegment())
                .isEqualTo(producer.getPoint().getSegment());

        // After unloading the connector, the producer and consumer should have different segments
        TestUtil.unloadChunk(connectorChunk).join();
        assertThat(consumer.getPoint().getSegment())
                .isNotEqualTo(producer.getPoint().getSegment());

        // When the chunk is reloaded, all should have the same segment again
        TestUtil.loadChunk(connectorChunk).join();
        FluidConnector reloadedConnector = BlockStorage.getAs(FluidConnector.class, connectorBlock);
        assertThat(consumer.getPoint().getSegment())
                .isEqualTo(reloadedConnector.getPoint().getSegment())
                .isEqualTo(producer.getPoint().getSegment());

        TestUtil.unloadChunk(producerChunk).join();
        TestUtil.unloadChunk(connectorChunk).join();
        TestUtil.unloadChunk(consumerChunk).join();
    }
}
