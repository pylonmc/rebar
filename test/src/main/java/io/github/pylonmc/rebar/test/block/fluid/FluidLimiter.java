package io.github.pylonmc.rebar.test.block.fluid;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarFluidTank;
import io.github.pylonmc.rebar.block.base.RebarUnloadBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.datatypes.RebarSerializers;
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent;
import io.github.pylonmc.rebar.fluid.FluidManager;
import io.github.pylonmc.rebar.fluid.FluidPointType;
import io.github.pylonmc.rebar.fluid.RebarFluid;
import io.github.pylonmc.rebar.fluid.VirtualFluidPoint;
import io.github.pylonmc.rebar.test.RebarTest;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;


public class FluidLimiter extends RebarBlock implements RebarFluidTank, RebarUnloadBlock {

    public static final NamespacedKey KEY = RebarTest.key("fluid_limiter");
    private static final NamespacedKey INPUT_KEY = RebarTest.key("input");
    private static final NamespacedKey OUTPUT_KEY = RebarTest.key("output");

    public static final double MAX_FLOW_RATE = 50.0;

    @Getter private final VirtualFluidPoint input;
    @Getter private final VirtualFluidPoint output;

    @SuppressWarnings("unused")
    public FluidLimiter(Block block, BlockCreateContext context) {
        super(block);

        input = new VirtualFluidPoint(block, FluidPointType.INPUT);
        output = new VirtualFluidPoint(block, FluidPointType.OUTPUT);

        FluidManager.add(input);
        FluidManager.add(output);
    }

    @SuppressWarnings("unused")
    public FluidLimiter(Block block, PersistentDataContainer pdc) {
        super(block);

        input = pdc.get(INPUT_KEY, RebarSerializers.FLUID_CONNECTION_POINT);
        output = pdc.get(OUTPUT_KEY, RebarSerializers.FLUID_CONNECTION_POINT);

        FluidManager.add(input);
        FluidManager.add(output);
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(INPUT_KEY, RebarSerializers.FLUID_CONNECTION_POINT, input);
        pdc.set(OUTPUT_KEY, RebarSerializers.FLUID_CONNECTION_POINT, output);
    }

    @Override
    public void onUnload(@NotNull RebarBlockUnloadEvent event, @NotNull org.bukkit.event.EventPriority priority) {
        FluidManager.remove(input);
        FluidManager.remove(output);
    }

    @Override
    public boolean isAllowedFluid(@NotNull RebarFluid fluid) {
        return true;
    }
}
