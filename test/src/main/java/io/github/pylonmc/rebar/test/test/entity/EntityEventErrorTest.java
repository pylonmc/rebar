package io.github.pylonmc.rebar.test.test.entity;

import io.github.pylonmc.rebar.config.RebarConfig;
import io.github.pylonmc.rebar.entity.EntityStorage;
import io.github.pylonmc.rebar.gametest.GameTestConfig;
import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.test.base.GameTest;
import io.github.pylonmc.rebar.test.entity.EntityEventError;
import org.bukkit.NamespacedKey;

import java.util.UUID;

public class EntityEventErrorTest extends GameTest {
    public EntityEventErrorTest(){
        super(new GameTestConfig.Builder(new NamespacedKey(RebarTest.instance(), "entity_event_error_test"))
                .size(1)
                .setUp(test -> {
                    RebarConfig.FULL_ERROR_STACK_TRACES = false;
                    EntityEventError entity = new EntityEventError(test.location());
                    EntityStorage.add(entity);
                    UUID entityUUID = entity.getUuid();
                    for(int i = 0; i < RebarConfig.ALLOWED_ENTITY_ERRORS + 1; i++){
                        // Yes, this is cursed, yes it works.
                        entity.getEntity().damage(0);
                    }
                    test.succeedWhen(() -> !EntityStorage.isRebarEntity(entityUUID));
                })
                .cleanup(test -> RebarConfig.FULL_ERROR_STACK_TRACES = true)
                .build());
    }
}
