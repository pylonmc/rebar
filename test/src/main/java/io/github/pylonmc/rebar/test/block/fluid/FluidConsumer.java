package io.github.pylonmc.rebar.test.block.fluid;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarFluidBufferBlock;
import io.github.pylonmc.rebar.block.base.RebarUnloadBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.datatypes.RebarSerializers;
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent;
import io.github.pylonmc.rebar.fluid.FluidManager;
import io.github.pylonmc.rebar.fluid.FluidPointType;
import io.github.pylonmc.rebar.fluid.RebarFluid;
import io.github.pylonmc.rebar.fluid.VirtualFluidPoint;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.fluid.Fluids;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class FluidConsumer extends RebarBlock implements RebarFluidBufferBlock, RebarUnloadBlock {

    public static final NamespacedKey LAVA_CONSUMER_KEY = RebarTest.key("lava_consumer");
    public static final NamespacedKey WATER_CONSUMER_KEY = RebarTest.key("water_consumer");

    private static final NamespacedKey pointKey = RebarTest.key("point");

    private static final double CAPACITY = 100.0;

    @Getter private final VirtualFluidPoint point;

    @SuppressWarnings("unused")
    public FluidConsumer(Block block, BlockCreateContext context) {
        super(block);
        point = new VirtualFluidPoint(block, FluidPointType.INPUT);
        FluidManager.add(point);
        createFluidBuffer(getFluidType(), CAPACITY, true, false);
    }

    @SuppressWarnings("unused")
    public FluidConsumer(Block block, PersistentDataContainer pdc) {
        super(block);
        point = pdc.get(pointKey, RebarSerializers.FLUID_CONNECTION_POINT);
        FluidManager.add(point);
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(pointKey, RebarSerializers.FLUID_CONNECTION_POINT, point);
    }

    @Override
    public void onUnload(@NotNull RebarBlockUnloadEvent event, @NotNull org.bukkit.event.EventPriority priority) {
        FluidManager.remove(point);
    }

    public double getAmount() {
        return fluidAmount(getFluidType());
    }

    private RebarFluid getFluidType() {
        return Map.of(
                LAVA_CONSUMER_KEY, Fluids.LAVA,
                WATER_CONSUMER_KEY, Fluids.WATER
        ).get(getKey());
    }
}
