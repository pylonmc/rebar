package io.github.pylonmc.rebar.test.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarBell;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.block.BellRingEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public class BlockEventError extends RebarBlock implements RebarBell {
    public static final NamespacedKey KEY = new NamespacedKey(RebarTest.instance(), "block_event_error");
    public BlockEventError(Block block, BlockCreateContext context){
        super(block);
    }
    public BlockEventError(Block block, PersistentDataContainer pdc){
        super(block);
    }

    @Override
    public void onRing(@NotNull BellRingEvent event, @NotNull org.bukkit.event.EventPriority priority) {
        throw new RuntimeException("This exception is thrown as part of a test");
    }
}
