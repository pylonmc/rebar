package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.electricity.ElectricityManager;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConsumerNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricProducerNode;

public class SimpleElectricNetworkTest extends ElectricityTest {

    @SuppressWarnings("deprecation")
    @Override
    protected void test() {
        ElectricityManager.clear();
        ElectricProducerNode producer = new ElectricProducerNode("", POSITION, 0);
        ElectricConsumerNode consumer = new ElectricConsumerNode("", POSITION, 10);
        ElectricityManager.addNode(producer);
        ElectricityManager.addNode(consumer);

        assertConsumer(consumer).isNotPowered();

        producer.connect(consumer);
        assertConsumer(consumer).isNotPowered();

        ElectricityManager.tick();
        assertConsumer(consumer).isNotPowered();

        producer.setPower(10);
        ElectricityManager.tick();
        assertConsumer(consumer).isPowered();
    }
}
