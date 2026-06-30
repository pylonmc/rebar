package io.github.pylonmc.rebar.test.electricity;

import io.github.pylonmc.rebar.electricity.ElectricityManager;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConnectorNode;

import static org.assertj.core.api.Assertions.assertThat;

public class MergeElectricNetworkTest extends ElectricityTest {
    @SuppressWarnings("deprecation")
    @Override
    protected void test() {
        ElectricityManager.clear();

        ElectricConnectorNode node1 = new ElectricConnectorNode("", POSITION);
        ElectricConnectorNode node2 = new ElectricConnectorNode("", POSITION);
        ElectricConnectorNode node3 = new ElectricConnectorNode("", POSITION);
        ElectricConnectorNode node4 = new ElectricConnectorNode("", POSITION);
        ElectricityManager.addNode(node1);
        ElectricityManager.addNode(node2);
        ElectricityManager.addNode(node3);
        ElectricityManager.addNode(node4);

        assertThat(ElectricityManager.getNetworks().size()).isEqualTo(4);

        node1.connect(node2);
        assertThat(ElectricityManager.getNetworks().size()).isEqualTo(3);

        node3.connect(node4);
        assertThat(ElectricityManager.getNetworks().size()).isEqualTo(2);

        node1.connect(node4);
        assertThat(ElectricityManager.getNetworks().size()).isEqualTo(1);

        node4.disconnectAll();
        ElectricityManager.removeNode(node4);
        assertThat(ElectricityManager.getNetworks().size()).isEqualTo(2);
    }
}
