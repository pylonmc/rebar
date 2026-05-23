package io.github.pylonmc.rebar.test.test.entity;

import io.github.pylonmc.rebar.entity.EntityStorage;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.entity.SimpleEntity;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


public class EntityStorageChunkReloadTest extends AsyncTest {

    @Override
    protected void test() {
        Chunk chunk = TestUtil.getRandomChunk(false).join();
        Location location = chunk.getBlock(5, 100, 5).getLocation();
        UUID uuid = TestUtil.runSync(() -> {
            SimpleEntity rebarEntity = new SimpleEntity(location);
            EntityStorage.add(rebarEntity);
            return rebarEntity.getUuid();
        }).join();

        assertThat(EntityStorage.isRebarEntity(uuid))
                .isTrue();

        TestUtil.unloadChunk(chunk).join();
        TestUtil.waitUntil(() -> !chunk.isEntitiesLoaded()).join();

        assertThat(EntityStorage.isRebarEntity(uuid))
                .isFalse();

        TestUtil.loadChunk(chunk).join();
        TestUtil.waitUntil(chunk::isEntitiesLoaded).join();

        assertThat(EntityStorage.isRebarEntity(uuid))
                .isTrue();
        assertThat(EntityStorage.get(uuid))
                .isNotNull()
                .isInstanceOf(SimpleEntity.class);
        assertThat(EntityStorage.getAs(SimpleEntity.class, uuid))
                .isNotNull()
                .extracting(SimpleEntity::getSomeQuantity)
                .isEqualTo(69);
        assertThat(((LivingEntity) EntityStorage.get(uuid).getEntity()).hasAI())
                .isFalse();

        TestUtil.unloadChunk(chunk).join();
    }
}
