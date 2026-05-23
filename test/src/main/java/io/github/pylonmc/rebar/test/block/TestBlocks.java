package io.github.pylonmc.rebar.test.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.block.fluid.FluidConnector;
import io.github.pylonmc.rebar.test.block.fluid.FluidConsumer;
import io.github.pylonmc.rebar.test.block.fluid.FluidLimiter;
import io.github.pylonmc.rebar.test.block.fluid.FluidProducer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;


public final class TestBlocks {

    private TestBlocks() {}

    public static final NamespacedKey SIMPLE_BLOCK_KEY = RebarTest.key("simple_block");

    public static void register() {
        RebarBlock.register(SIMPLE_BLOCK_KEY, Material.AMETHYST_BLOCK, RebarBlock.class);
        RebarBlock.register(BlockWithField.KEY, Material.AMETHYST_BLOCK, BlockWithField.class);
        RebarBlock.register(TestRebarSimpleMultiblock.KEY, Material.AMETHYST_BLOCK, TestRebarSimpleMultiblock.class);
        RebarBlock.register(TickingErrorBlock.KEY, Material.AMETHYST_BLOCK, TickingErrorBlock.class);
        RebarBlock.register(TickingBlock.KEY, Material.AMETHYST_BLOCK, TickingBlock.class);
        RebarBlock.register(FluidConsumer.LAVA_CONSUMER_KEY, Material.AMETHYST_BLOCK, FluidConsumer.class);
        RebarBlock.register(FluidConsumer.WATER_CONSUMER_KEY, Material.AMETHYST_BLOCK, FluidConsumer.class);
        RebarBlock.register(FluidProducer.LAVA_PRODUCER_KEY, Material.AMETHYST_BLOCK, FluidProducer.class);
        RebarBlock.register(FluidProducer.WATER_PRODUCER_KEY, Material.AMETHYST_BLOCK, FluidProducer.class);
        RebarBlock.register(FluidLimiter.KEY, Material.AMETHYST_BLOCK, FluidLimiter.class);
        RebarBlock.register(FluidConnector.KEY, Material.AMETHYST_BLOCK, FluidConnector.class);
        RebarBlock.register(BlockEventError.KEY, Material.TARGET, BlockEventError.class);
    }
}
