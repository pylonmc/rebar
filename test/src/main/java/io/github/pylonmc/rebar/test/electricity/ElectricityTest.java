package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.electricity.nodes.ElectricConsumerNode;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.util.position.BlockPosition;
import org.assertj.core.api.AssertDelegateTarget;
import org.bukkit.Bukkit;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ElectricityTest extends AsyncTest {

    protected static final BlockPosition POSITION = new BlockPosition(Bukkit.getWorld("gametests"), 0, 0, 0);

    protected static ConsumerAssertion assertConsumer(ElectricConsumerNode consumer) {
        return new ConsumerAssertion(consumer);
    }

    protected record ConsumerAssertion(ElectricConsumerNode consumer) implements AssertDelegateTarget {
        void isPowered() {
            assertThat(consumer.isPowered()).isTrue();
        }

        void isNotPowered() {
            assertThat(consumer.isPowered()).isFalse();
        }
    }
}
