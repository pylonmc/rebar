package io.github.pylonmc.rebar.test.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.test.RebarTest;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;


public class BlockWithField extends RebarBlock {

    public static final NamespacedKey KEY = RebarTest.key("block_with_field");
    public static final NamespacedKey PROGRESS_KEY = RebarTest.key("progress");

    @Getter private final int progress;

    @SuppressWarnings("unused")
    public BlockWithField(Block block, BlockCreateContext context) {
        super(block, context);
        progress = 240;
    }

    @SuppressWarnings({"unused", "DataFlowIssue"})
    public BlockWithField(Block block, PersistentDataContainer pdc) {
        super(block, pdc);
        progress = pdc.get(PROGRESS_KEY, PersistentDataType.INTEGER);
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(PROGRESS_KEY, PersistentDataType.INTEGER, 130);
    }
}