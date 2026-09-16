package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.electricity.ElectricNetwork;
import io.github.pylonmc.rebar.electricity.ElectricityManager;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConsumerNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricProducerNode;

import static org.assertj.core.api.Assertions.assertThat;

// https://discord.com/channels/1329177304857055273/1329177972631928915/1544008087382261831
public class SeriesConsumerElectricNetworkTest extends ElectricityTest {

    @SuppressWarnings("deprecation")
    @Override
    protected void test() {
        ElectricityManager.clear();
        ElectricProducerNode producer = new ElectricProducerNode("", POSITION, 0);
        ElectricConsumerNode consumer1 = new ElectricConsumerNode("", POSITION, 10);
        ElectricConsumerNode consumer2 = new ElectricConsumerNode("", POSITION, 10);
        ElectricityManager.addNode(producer);
        ElectricityManager.addNode(consumer1);
        ElectricityManager.addNode(consumer2);

        producer.setPower(1000000);
        producer.connect(consumer1);
        new ElectricNetwork.Edge(producer, consumer1).setPowerLimit(50);
        ElectricityManager.tick();
        assertThat(consumer1.isPowered()).isTrue();

        consumer1.connect(consumer2);
        new ElectricNetwork.Edge(consumer1, consumer2).setPowerLimit(50);
        ElectricityManager.tick();
        assertThat(consumer1.isPowered()).isTrue();
        assertThat(consumer2.isPowered()).isTrue();
    }
}
