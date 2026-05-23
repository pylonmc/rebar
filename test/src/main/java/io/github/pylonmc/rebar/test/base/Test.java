package io.github.pylonmc.rebar.test.base;

import io.github.pylonmc.rebar.test.RebarTest;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;


public interface Test {
    default TestResult start() {
        RebarTest.instance().getLogger().log(Level.INFO, "Test %s started".formatted(getKey()));
        return run();
    }

    TestResult run();

    default NamespacedKey getKey() {
        return new NamespacedKey(RebarTest.instance(), getClass().getSimpleName());
    }

    default @NotNull TestResult onComplete(@Nullable Throwable e, Instant startTime) {
        long timeTakenMillis = Duration.between(startTime, Instant.now()).toMillis();
        if (e != null) {
            RebarTest.instance().getLogger().log(Level.INFO, "Test %s failed".formatted(getKey()), e);
        } else {
            RebarTest.instance().getLogger().log(Level.INFO, "Test %s passed in %dms".formatted(getKey(), timeTakenMillis));
        }
        return new TestResult(getKey(), e == null, timeTakenMillis);
    }
}
