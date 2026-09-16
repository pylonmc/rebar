package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.test.base.SyncTest;
import io.github.pylonmc.rebar.util.position.BlockPosition;
import org.bukkit.Bukkit;

public abstract class ElectricityTest extends SyncTest {

    protected static final BlockPosition POSITION = new BlockPosition(Bukkit.getWorld("gametests"), 0, 0, 0);
}
