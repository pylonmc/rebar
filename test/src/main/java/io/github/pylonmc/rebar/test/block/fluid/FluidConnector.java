package io.github.pylonmc.rebar.test.block.fluid;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarUnloadBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.datatypes.RebarSerializers;
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent;
import io.github.pylonmc.rebar.fluid.FluidManager;
import io.github.pylonmc.rebar.fluid.FluidPointType;
import io.github.pylonmc.rebar.fluid.VirtualFluidPoint;
import io.github.pylonmc.rebar.test.RebarTest;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;


public class FluidConnector extends RebarBlock implements RebarUnloadBlock {

    public static final NamespacedKey KEY = RebarTest.key("fluid_connector");
    private static final NamespacedKey POINT_KEY = RebarTest.key("point");

    @Getter
    private final VirtualFluidPoint point;

    @SuppressWarnings("unused")
    public FluidConnector(Block block, BlockCreateContext context) {
        super(block);
        point = new VirtualFluidPoint(block, FluidPointType.INTERSECTION);
        FluidManager.add(point);
    }

    @SuppressWarnings("unused")
    public FluidConnector(Block block, PersistentDataContainer pdc) {
        super(block);
        point = pdc.get(POINT_KEY, RebarSerializers.FLUID_CONNECTION_POINT);
        FluidManager.add(point);
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(POINT_KEY, RebarSerializers.FLUID_CONNECTION_POINT, point);
    }

    @Override
    public void onUnload(@NotNull RebarBlockUnloadEvent event, @NotNull org.bukkit.event.EventPriority priority) {
        FluidManager.remove(point);
    }
}
