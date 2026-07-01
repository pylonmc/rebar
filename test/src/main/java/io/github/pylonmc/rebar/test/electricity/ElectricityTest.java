package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.util.position.BlockPosition;
import org.bukkit.Bukkit;

public abstract class ElectricityTest extends AsyncTest {

    protected static final BlockPosition POSITION = new BlockPosition(Bukkit.getWorld("gametests"), 0, 0, 0);
}
