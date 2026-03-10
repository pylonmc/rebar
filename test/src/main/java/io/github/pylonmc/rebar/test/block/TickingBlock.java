package io.github.pylonmc.rebar.test.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarTickingBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;


public class TickingBlock extends RebarBlock implements RebarTickingBlock {

    public static final NamespacedKey KEY = RebarTest.key("ticking_block");

    public int ticks = 0;

    @SuppressWarnings("unused")
    public TickingBlock(Block block, BlockCreateContext context) {
        super(block, context);
    }

    @SuppressWarnings("unused")
    public TickingBlock(Block block, PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    public void tick() {
        ticks++;
    }
}