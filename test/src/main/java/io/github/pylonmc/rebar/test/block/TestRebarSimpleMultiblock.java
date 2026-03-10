package io.github.pylonmc.rebar.test.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarSimpleMultiblock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.util.Map;


public class TestRebarSimpleMultiblock extends RebarBlock implements RebarSimpleMultiblock {

    public static final NamespacedKey KEY = RebarTest.key("simple_multiblock");

    @SuppressWarnings("unused")
    public TestRebarSimpleMultiblock(Block block, BlockCreateContext context) {
        super(block, context);
    }

    @SuppressWarnings("unused")
    public TestRebarSimpleMultiblock(Block block, PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    public @NotNull Map<Vector3i, MultiblockComponent> getComponents() {
        return Map.of(
                new Vector3i(1, 1, 4), new RebarMultiblockComponent(Blocks.SIMPLE_BLOCK_KEY),
                new Vector3i(2, -1, 0), new RebarMultiblockComponent(Blocks.SIMPLE_BLOCK_KEY)
        );
    }
}