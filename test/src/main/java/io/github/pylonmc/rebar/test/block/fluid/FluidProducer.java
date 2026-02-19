package io.github.pylonmc.rebar.test.block.fluid;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarFluidBlock;
import io.github.pylonmc.rebar.block.base.RebarUnloadBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.config.RebarConfig;
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


public class FluidProducer extends RebarBlock implements RebarFluidBlock, RebarUnloadBlock {

    public static final NamespacedKey LAVA_PRODUCER_KEY = RebarTest.key("lava_producer");
    public static final NamespacedKey WATER_PRODUCER_KEY = RebarTest.key("water_producer");

    public static final double FLUID_PER_SECOND = 200.0;

    private final NamespacedKey pointKey = RebarTest.key("point");
    @Getter private final VirtualFluidPoint point;

    @SuppressWarnings("unused")
    public FluidProducer(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block);
        point = new VirtualFluidPoint(block, FluidPointType.OUTPUT);
        FluidManager.add(point);
    }

    @SuppressWarnings("unused")
    public FluidProducer(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
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

    @Override
    public @NotNull Map<RebarFluid, Double> getSuppliedFluids() {
        return Map.of(
                getFluidType(), FLUID_PER_SECOND * RebarConfig.FLUID_TICK_INTERVAL / 20.0
        );
    }

    @Override
    public void onFluidRemoved(@NotNull RebarFluid fluid, double amount) {
        // do nothing
    }

    private RebarFluid getFluidType() {
        return Map.of(
                LAVA_PRODUCER_KEY, Fluids.LAVA,
                WATER_PRODUCER_KEY, Fluids.WATER
        ).get(getKey());
    }
}
