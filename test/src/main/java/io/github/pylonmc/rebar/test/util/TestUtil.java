package io.github.pylonmc.rebar.test.util;

import io.github.pylonmc.rebar.test.RebarTest;
import io.github.pylonmc.rebar.util.position.ChunkPosition;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;


public final class TestUtil {

    private static final Random random = new Random();

    private TestUtil() {}

    @CheckReturnValue
    public static <T> @NotNull CompletableFuture<T> runSync(@NotNull Callable<T> callable, int delayTicks) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskLater(RebarTest.instance(), () -> {
            try {
                future.complete(callable.call());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }, delayTicks);
        return future;
    }

    @CheckReturnValue
    public static <T> @NotNull CompletableFuture<T> runSync(@NotNull Callable<T> callable) {
        return runSync(callable, 0);
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> runSync(@NotNull Runnable runnable, int delayTicks) {
        return runSync(() -> {
            runnable.run();
            return null;
        }, delayTicks);
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> runSync(@NotNull Runnable runnable) {
        return runSync(runnable, 0);
    }

    @CheckReturnValue
    public static <T> @NotNull CompletableFuture<T> runAsync(@NotNull Callable<T> callable, int delayTicks) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskLaterAsynchronously(RebarTest.instance(), () -> {
            try {
                future.complete(callable.call());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }, delayTicks);
        return future;
    }

    @CheckReturnValue
    public static <T> @NotNull CompletableFuture<T> runAsync(@NotNull Callable<T> callable) {
        return runAsync(callable, 0);
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> runAsync(@NotNull Runnable runnable, int delayTicks) {
        return runAsync(() -> {
            runnable.run();
            return null;
        }, delayTicks);
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> runAsync(@NotNull Runnable runnable) {
        return runAsync(runnable, 0);
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Chunk> getRandomChunk(boolean waitForUnload) {
        return runAsync(() -> {
            double max = 10000;
            int x = (int) random.nextDouble(-max, max);
            int z = (int) random.nextDouble(-max, max);
            Chunk chunk = RebarTest.testWorld.getChunkAt(x, z);

            if (waitForUnload) {
                while (chunk.isLoaded()) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                chunk.setForceLoaded(true);
            }

            return chunk;
        });
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<List<Chunk>> getRandomChunks(int count, boolean waitForUnload) {
        return runAsync(() -> {
            List<CompletableFuture<Chunk>> futures = new ArrayList<>();
            //noinspection Convert2streamapi
            for (int i = 0; i < count; i++) {
                futures.add(getRandomChunk(waitForUnload));
            }

            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        });
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> sleepTicks(int ticks) {
        return runAsync(() -> null, ticks);
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> waitUntil(BooleanSupplier supplier, int intervalTicks) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskTimer(RebarTest.instance(), task -> {
            if (supplier.getAsBoolean()) {
                future.complete(null);
                task.cancel();
            }
        }, 0, intervalTicks);
        return future;
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> waitUntil(BooleanSupplier supplier) {
        return waitUntil(supplier, 1);
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> loadChunk(@NotNull Chunk chunk) {
        return runAsync(() -> {
            runSync(() -> {
                chunk.load();
                chunk.setForceLoaded(true);
            }).join();
            waitUntil(chunk::isLoaded).join();
        });
    }

    @CheckReturnValue
    public static @NotNull CompletableFuture<Void> unloadChunk(@NotNull Chunk chunk) {
        return runAsync(() -> {
            ChunkPosition chunkPosition = new ChunkPosition(chunk);
            runSync(() -> {
                chunk.setForceLoaded(false);
                chunk.unload();
            }).join();
            waitUntil(() -> !chunkPosition.isLoaded()).join();
        });
    }
}
