package io.github.pylonmc.rebar.test.test.serializer;

import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.SyncTest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class SerializerTest<T> extends SyncTest {
    private final T value;
    private final PersistentDataType<?, T> type;

    protected SerializerTest(T value, PersistentDataType<?, T> type) {
        super();
        this.value = value;
        this.type = type;
    }

    @Override
    public void test() {
        NamespacedKey key = new NamespacedKey(RebarTest.instance(), "key");

        ItemStack stack = ItemStack.of(Material.ACACIA_BOAT);
        stack.editMeta(meta -> meta.getPersistentDataContainer()
                .set(key, type, value));
        assertThat(stack.getPersistentDataContainer().get(key, type))
                .isEqualTo(value);
    }
}
