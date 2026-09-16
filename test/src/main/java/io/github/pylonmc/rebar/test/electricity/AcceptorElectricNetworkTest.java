package io.github.pylonmc.rebar.test.electricity;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.pylonmc.rebar.electricity.ElectricityManager;
import io.github.pylonmc.rebar.electricity.nodes.ElectricAcceptorNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricProducerNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

public class AcceptorElectricNetworkTest extends ElectricityTest {

    @SuppressWarnings("deprecation")
    @Override
    protected void test() {
        ElectricityManager.clear();
        ElectricProducerNode producer = new ElectricProducerNode("", POSITION, 0);
        ElectricAcceptorNode acceptor = new ElectricAcceptorNode("", POSITION);
        ElectricityManager.addNode(producer);
        ElectricityManager.addNode(acceptor);

        // atomic cause java requires lambda captures to be effectively final
        AtomicDouble acceptedPower = new AtomicDouble();
        acceptor.onAccept(power -> {
            acceptedPower.addAndGet(power);
            return power;
        });

        producer.setPower(50);
        producer.connect(acceptor);
        ElectricityManager.tick();
        assertThat(acceptedPower.get()).isCloseTo(50, withinPercentage(1));
    }
}
