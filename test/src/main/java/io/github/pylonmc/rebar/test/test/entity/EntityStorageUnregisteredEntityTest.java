package io.github.pylonmc.rebar.test.test.entity;

import io.github.pylonmc.rebar.entity.EntityStorage;
import io.github.pylonmc.rebar.entity.RebarEntity;
import io.github.pylonmc.rebar.registry.RebarRegistry;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.test.util.TestUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


public class EntityStorageUnregisteredEntityTest extends AsyncTest {

    public static class UnregisteredEntity extends RebarEntity<LivingEntity> {

        public static final NamespacedKey KEY = RebarTest.key("unregistered_entity");

        public UnregisteredEntity(@NotNull Location location) {
            super(KEY, location.getWorld().spawn(location, Skeleton.class));
        }

        @SuppressWarnings("unused")
        public UnregisteredEntity(@NotNull LivingEntity entity) {
            super(entity);
        }
    }

    @Override
    protected void test() {
        TestUtil.runSync(() -> {
            RebarEntity.register(UnregisteredEntity.KEY, LivingEntity.class, UnregisteredEntity.class);
        }).join();

        Chunk chunk = TestUtil.getRandomChunk(false).join();
        Location location = chunk.getBlock(5, 100, 5).getLocation();
        UUID uuid = TestUtil.runSync(() -> {
            UnregisteredEntity rebarEntity = new UnregisteredEntity(location);
            EntityStorage.add(rebarEntity);
            return rebarEntity.getUuid();
        }).join();

        assertThat(EntityStorage.isRebarEntity(uuid))
                .isTrue();
        assertThat(EntityStorage.get(uuid))
                .isNotNull()
                .isInstanceOf(UnregisteredEntity.class);
        TestUtil.unloadChunk(chunk).join();
        TestUtil.waitUntil(() -> !chunk.isEntitiesLoaded()).join();

        TestUtil.runSync(() -> {
            RebarRegistry.ENTITIES.unregister(UnregisteredEntity.KEY);
        }).join();

        TestUtil.loadChunk(chunk).join();
        TestUtil.waitUntil(chunk::isEntitiesLoaded).join();
        assertThat(EntityStorage.isRebarEntity(uuid))
                .isFalse();
        assertThat(EntityStorage.get(uuid))
                .isNull();
        TestUtil.unloadChunk(chunk).join();
        TestUtil.waitUntil(() -> !chunk.isEntitiesLoaded()).join();

        TestUtil.runSync(() -> {
            RebarEntity.register(UnregisteredEntity.KEY, LivingEntity.class, UnregisteredEntity.class);
        }).join();

        TestUtil.loadChunk(chunk).join();
        TestUtil.waitUntil(chunk::isEntitiesLoaded).join();
        assertThat(EntityStorage.isRebarEntity(uuid))
                .isTrue();
        assertThat(EntityStorage.get(uuid))
                .isNotNull()
                .isInstanceOf(UnregisteredEntity.class);
        TestUtil.unloadChunk(chunk).join();
    }
}
