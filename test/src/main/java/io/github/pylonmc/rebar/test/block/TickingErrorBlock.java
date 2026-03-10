package io.github.pylonmc.rebar.test.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarTickingBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;


public class TickingErrorBlock extends RebarBlock implements RebarTickingBlock {

    public static final NamespacedKey KEY = RebarTest.key("ticking_error_block");

    @SuppressWarnings("unused")
    public TickingErrorBlock(Block block, BlockCreateContext context) {
        super(block, context);
    }

    @SuppressWarnings("unused")
    public TickingErrorBlock(Block block, PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    public void tick() {
        throw new RuntimeException("This exception is thrown as part of a test");
    }
}