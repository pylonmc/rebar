package io.github.pylonmc.rebar.test;

import io.github.pylonmc.rebar.addon.RebarAddon;
import io.github.pylonmc.rebar.config.RebarConfig;
import io.github.pylonmc.rebar.test.base.Test;
import io.github.pylonmc.rebar.test.base.TestResult;
import io.github.pylonmc.rebar.test.block.TestBlocks;
import io.github.pylonmc.rebar.test.entity.TestEntities;
import io.github.pylonmc.rebar.test.fluid.TestFluids;
import io.github.pylonmc.rebar.test.item.TestItems;
import io.github.pylonmc.rebar.test.test.block.*;
import io.github.pylonmc.rebar.test.test.entity.EntityEventErrorTest;
import io.github.pylonmc.rebar.test.test.entity.EntityStorageChunkReloadTest;
import io.github.pylonmc.rebar.test.test.entity.EntityStorageSimpleTest;
import io.github.pylonmc.rebar.test.test.entity.EntityStorageUnregisteredEntityTest;
import io.github.pylonmc.rebar.test.test.fluid.*;
import io.github.pylonmc.rebar.test.test.item.RebarItemStackInterfaceTest;
import io.github.pylonmc.rebar.test.test.misc.GametestTest;
import io.github.pylonmc.rebar.test.test.recipe.CraftingTest;
import io.github.pylonmc.rebar.test.test.recipe.FurnaceTest;
import io.github.pylonmc.rebar.test.test.serializer.*;
import io.github.pylonmc.rebar.test.util.BedrockWorldGenerator;
import io.github.pylonmc.rebar.test.util.TestUtil;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RebarTest extends JavaPlugin implements RebarAddon {
    @Accessors(fluent = true)
    @Getter
    private static RebarTest instance;
    public static World testWorld;
    private static final boolean MANUAL_SHUTDOWN = Boolean.parseBoolean(System.getenv("MANUAL_SHUTDOWN"));

    private static @NotNull List<Test> initTests() {
        List<Test> tests = new ArrayList<>();

        tests.add(new BlockStorageAddTest());
        tests.add(new BlockStorageChunkReloadTest());
        tests.add(new BlockStorageFilledChunkTest());
        tests.add(new BlockStorageMissingSchemaTest());
        tests.add(new BlockStorageRemoveTest());
        tests.add(new SimpleMultiblockTest());
        tests.add(new SimpleMultiblockRotatedTest());
        tests.add(new TickingBlockTest());
        tests.add(new TickingBlockErrorTest());
        tests.add(new BlockEventErrorTest());

        tests.add(new RebarItemStackInterfaceTest());

        tests.add(new GametestTest());

        tests.add(new SerializerTestBlockPosition());
        tests.add(new SerializerTestBlockPositionNoWorld());
        tests.add(new SerializerTestChar());
        tests.add(new SerializerTestChunkPosition());
        tests.add(new SerializerTestChunkPositionNoWorld());
        tests.add(new SerializerTestEnum());
        tests.add(new SerializerTestItemStack());
        tests.add(new SerializerTestLocation());
        tests.add(new SerializerTestNamespacedKey());
        tests.add(new SerializerTestSetOfInts());
        tests.add(new SerializerTestSetOfSetOfStrings());
        tests.add(new SerializerTestUUID());
        tests.add(new SerializerTestVector());

        tests.add(new CraftingTest());
        tests.add(new FurnaceTest());

        tests.add(new EntityStorageSimpleTest());
        tests.add(new EntityStorageUnregisteredEntityTest());
        tests.add(new EntityStorageChunkReloadTest());
        tests.add(new EntityEventErrorTest());

        tests.add(new FluidConnectionTest());
        tests.add(new FluidCyclicConnectionsTest());
        tests.add(new FluidTickerTest());
        tests.add(new FluidTickerTestWithMixedFluids());
        tests.add(new FluidPartialReloadTest());
        tests.add(new FluidFlowRateTest());
        tests.add(new FluidPredicateTest());

        return tests;
    }

    private static void run() {
        Logger logger = instance.getLogger();

        // Create world - can't do this on enable due to plugin loading on STARTUP rather than POSTWORLD
        TestUtil.runSync(() -> {
            World world = new WorldCreator("gametests")
                    .generator(new BedrockWorldGenerator())
                    .environment(World.Environment.NORMAL)
                    .createWorld();
            assert world != null; // shut up intellij // you can't make me, you fool. I will destroy you silly mortals.
            testWorld = world;

            testWorld.setGameRule(GameRules.SPAWN_MOBS, false);
            testWorld.setGameRule(GameRules.SPAWN_PATROLS, false);
            testWorld.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);

            try {
                // TODO get rid of these and convert registration to static blocks
                TestBlocks.register();
                TestItems.register();
                TestEntities.register();
                TestFluids.register();
            } catch (Exception e) {
                instance().getLogger().severe("Failed to set up tests");
                e.printStackTrace();
                communicateFailure();
                if (!MANUAL_SHUTDOWN) {
                    Bukkit.shutdown();
                }
            }
        }).join();

        // Tests must be initialised on main thread
        List<Test> tests = TestUtil.runSync(RebarTest::initTests).join();

        List<TestResult> results = tests.stream()
                .map(Test::start)
                .toList();

        List<NamespacedKey> succeeded = results.stream()
                .filter(TestResult::success)
                .map(TestResult::key)
                .toList();
        List<NamespacedKey> failed = results.stream()
                .filter(r -> !r.success())
                .map(TestResult::key)
                .toList();


        logger.info("[ ===== TEST SUMMARY ===== ]");
        logger.info("%s/%s TESTS PASSED"
                .formatted(succeeded.size(), succeeded.size() + failed.size()));

        if (failed.isEmpty()) {
            long totalTimeTakenMillis = results.stream()
                    .mapToLong(TestResult::timeTakenMillis)
                    .sum();
            logger.info("TIME TAKEN: %dms".formatted(totalTimeTakenMillis));
        }

        if (!failed.isEmpty()) {
            String failedString = failed.stream()
                    .map(NamespacedKey::toString)
                    .collect(Collectors.joining(", "));
            logger.info("FAILED: %s".formatted(failedString));

            communicateFailure();
        }

        if (!MANUAL_SHUTDOWN) {
            logger.info("Testing complete; shutting down server...");
            Bukkit.shutdown();
        }
    }

    private static void communicateFailure() {
        // Communicate back to the runServer task that the tests failed
        try {
            new File("tests-failed").createNewFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onEnable() {
        registerWithRebar();

        getLogger().info("Test addon enabled!");

        instance = this;

        if (Boolean.parseBoolean(System.getenv("NO_TEST"))) {
            return;
        }

        TestUtil.runAsync(RebarTest::run, 1);
    }

    @Override
    public @NotNull JavaPlugin getJavaPlugin() {
        return this;
    }

    public static @NotNull NamespacedKey key(String key) {
        return new NamespacedKey(instance, key);
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.WAXED_WEATHERED_CUT_COPPER_STAIRS;
    }

    @Override
    public @NotNull Locale getDefaultLanguage() {
        return Locale.of("none");
    }
}
