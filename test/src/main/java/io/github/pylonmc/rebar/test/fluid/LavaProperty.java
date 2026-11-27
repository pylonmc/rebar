package io.github.pylonmc.rebar.test.fluid;

import io.github.pylonmc.rebar.fluid.RebarFluidProperty;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;


public class LavaProperty implements RebarFluidProperty {
    @Override
    public @NotNull Component getDisplayText() {
        return Component.empty();
    }
}
