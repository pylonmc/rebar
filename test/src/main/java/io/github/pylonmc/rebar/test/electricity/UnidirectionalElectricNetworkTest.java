package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.electricity.ElectricNetwork;
import io.github.pylonmc.rebar.electricity.ElectricityManager;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConnectorNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConsumerNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricProducerNode;

import static org.assertj.core.api.Assertions.assertThat;

public class UnidirectionalElectricNetworkTest extends ElectricityTest {

    @SuppressWarnings("deprecation")
    @Override
    protected void test() {
        ElectricityManager.clear();
        ElectricProducerNode producer = new ElectricProducerNode("", POSITION, 0);
        ElectricConnectorNode connector = new ElectricConnectorNode("", POSITION);
        ElectricConsumerNode consumer = new ElectricConsumerNode("", POSITION, 10);
        ElectricityManager.addNode(producer);
        ElectricityManager.addNode(connector);
        ElectricityManager.addNode(consumer);

        ElectricNetwork.Edge edge = new ElectricNetwork.Edge(consumer, connector);

        producer.connect(connector);
        connector.connect(consumer);
        edge.setUnidirectional(true);
        assertThat(consumer.isPowered()).isFalse();

        ElectricityManager.tick();
        assertThat(consumer.isPowered()).isFalse();

        producer.setPower(10);
        ElectricityManager.tick();
        assertThat(consumer.isPowered()).isFalse();

        edge.setUnidirectional(false);
        ElectricityManager.tick();
        assertThat(consumer.isPowered()).isTrue();
    }
}
