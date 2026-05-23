package io.github.pylonmc.rebar.test.fluid;

import io.github.pylonmc.rebar.fluid.RebarFluid;
import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.Material;


public class TestFluids {

    public static final RebarFluid WATER = new RebarFluid(RebarTest.key("water"),  Material.CYAN_CONCRETE);
    public static final RebarFluid LAVA = new RebarFluid(RebarTest.key("lava"), Material.ORANGE_CONCRETE)
            .addTag(new LavaTag());

    public static void register() {
        WATER.register();
        LAVA.register();
    }
}
