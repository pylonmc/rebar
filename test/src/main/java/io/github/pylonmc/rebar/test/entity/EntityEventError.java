package io.github.pylonmc.rebar.test.entity;

import io.github.pylonmc.rebar.entity.RebarEntity;
import io.github.pylonmc.rebar.entity.interfaces.DamageRebarEntityHandler;
import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;


public class EntityEventError extends RebarEntity<LivingEntity> implements DamageRebarEntityHandler {

    public static final NamespacedKey KEY = RebarTest.key("entity_event_error");

    public EntityEventError(@NotNull Location location) {
        super(KEY, location.getWorld().spawn(location, Skeleton.class));
    }

    @SuppressWarnings({"unused", "DataFlowIssue"})
    public EntityEventError(@NotNull LivingEntity entity) {
        super(entity);
    }

    @Override
    public void onDamaged(@NotNull EntityDamageEvent event, @NotNull EventPriority priority) {
        throw new RuntimeException("This exception is thrown as part of a test");
    }
}
