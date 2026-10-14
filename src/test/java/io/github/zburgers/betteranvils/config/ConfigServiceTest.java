package io.github.zburgers.betteranvils.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void missingFileLoadsAndPersistsDefaults() throws IOException {
        var path = tempDir.resolve("better-anvils.json");
        var service = new ConfigService(path, ignored -> {});

        assertEquals(BetterAnvilsConfig.defaults(), service.load());
        assertTrue(Files.exists(path));
        assertEquals(BetterAnvilsConfig.defaults(), new ConfigService(path, ignored -> {}).load());
    }

    @Test
    void savedValuesRoundTripAcrossServiceInstances() throws IOException {
        var path = tempDir.resolve("better-anvils.json");
        var expected = new BetterAnvilsConfig(240, 75, 3, false);
        var service = new ConfigService(path, ignored -> {});

        service.load();
        service.save(expected);

        assertEquals(expected, new ConfigService(path, ignored -> {}).load());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{broken",
            "{\"maximumAnvilCost\":100.5,\"anvilDiscountPercent\":50,\"minimumAnvilCost\":1,\"preservePriorWorkPenalty\":true}",
            "{\"maximumAnvilCost\":100,\"anvilDiscountPercent\":\"50\",\"minimumAnvilCost\":1,\"preservePriorWorkPenalty\":true}",
            "{\"maximumAnvilCost\":100,\"anvilDiscountPercent\":50,\"minimumAnvilCost\":1,\"preservePriorWorkPenalty\":1}"
    })
    void malformedOrWronglyTypedFilesAreBackedUpBeforeDefaultsAreWritten(String json) throws IOException {
        var path = tempDir.resolve("better-anvils.json");
        Files.writeString(path, json);
        var warnings = new ArrayList<String>();

        assertEquals(BetterAnvilsConfig.defaults(), new ConfigService(path, warnings::add).load());
        assertTrue(Files.readString(path).contains("\"anvilDiscountPercent\": 50"));
        assertTrue(hasInvalidBackup());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void outOfRangeFileIsPreservedInBackupAndDefaultsAreRestored() throws IOException {
        var path = tempDir.resolve("better-anvils.json");
        var invalid = "{\"maximumAnvilCost\":100,\"anvilDiscountPercent\":50,\"minimumAnvilCost\":11,\"preservePriorWorkPenalty\":true}";
        Files.writeString(path, invalid);

        assertEquals(BetterAnvilsConfig.defaults(), new ConfigService(path, ignored -> {}).load());
        assertTrue(hasInvalidBackupWithContents(invalid));
    }

    @Test
    void failedSaveLeavesTheActiveConfigAndExistingTargetAlone() throws IOException {
        var blocker = tempDir.resolve("not-a-directory");
        Files.writeString(blocker, "keep");
        var service = new ConfigService(blocker.resolve("better-anvils.json"), ignored -> {});
        var desired = new BetterAnvilsConfig(200, 75, 1, false);

        assertThrows(IOException.class, () -> service.save(desired));
        assertEquals(BetterAnvilsConfig.defaults(), service.current());
        assertEquals("keep", Files.readString(blocker));
    }

    @Test
    void invalidDraftIsRejectedBeforeWriting() throws IOException {
        var path = tempDir.resolve("better-anvils.json");
        var service = new ConfigService(path, ignored -> {});
        service.load();
        var original = Files.readString(path);

        assertThrows(IllegalArgumentException.class, () -> service.save(new BetterAnvilsConfig(100, 50, 11, true)));
        assertEquals(original, Files.readString(path));
        assertEquals(BetterAnvilsConfig.defaults(), service.current());
    }

    private boolean hasInvalidBackup() throws IOException {
        try (var paths = Files.list(tempDir)) {
            return paths.anyMatch(candidate -> candidate.getFileName().toString().contains(".invalid-"));
        }
    }

    private boolean hasInvalidBackupWithContents(String expected) throws IOException {
        try (var paths = Files.list(tempDir)) {
            for (var candidate : paths.filter(path -> path.getFileName().toString().contains(".invalid-")).toList()) {
                if (Files.readString(candidate).equals(expected)) {
                    return true;
                }
            }
            return false;
        }
    }
}
