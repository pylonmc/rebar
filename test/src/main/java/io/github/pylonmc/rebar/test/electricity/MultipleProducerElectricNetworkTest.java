package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.electricity.ElectricityManager;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConsumerNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricProducerNode;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleProducerElectricNetworkTest extends ElectricityTest {

    @SuppressWarnings("deprecation")
    @Override
    protected void test() {
        ElectricityManager.clear();
        ElectricProducerNode producer = new ElectricProducerNode("", POSITION, 0);
        ElectricProducerNode producer2 = new ElectricProducerNode("", POSITION, 0);
        ElectricConsumerNode consumer = new ElectricConsumerNode("", POSITION, 10);
        ElectricityManager.addNode(producer);
        ElectricityManager.addNode(producer2);
        ElectricityManager.addNode(consumer);

        producer.connect(consumer);
        producer2.connect(consumer);
        assertThat(consumer.isPowered()).isFalse();

        ElectricityManager.tick();
        assertThat(consumer.isPowered()).isFalse();

        producer.setPower(5);
        ElectricityManager.tick();
        assertThat(consumer.isPowered()).isFalse();

        producer2.setPower(5);
        ElectricityManager.tick();
        assertThat(consumer.isPowered()).isTrue();

        ElectricityManager.tick();
        assertThat(consumer.isPowered()).isTrue();
    }
}
