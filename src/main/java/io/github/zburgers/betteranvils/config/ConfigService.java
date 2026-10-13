package io.github.zburgers.betteranvils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

public final class ConfigService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Consumer<String> warningSink;
    private BetterAnvilsConfig active = BetterAnvilsConfig.defaults();

    public ConfigService(Path file, Consumer<String> warningSink) {
        this.file = Objects.requireNonNull(file).toAbsolutePath();
        this.warningSink = Objects.requireNonNull(warningSink);
    }

    public synchronized BetterAnvilsConfig load() {
        if (!Files.exists(file)) {
            active = BetterAnvilsConfig.defaults();
            try {
                writeAtomically(active);
            } catch (IOException e) {
                warningSink.accept("Could not create " + file + ": " + e.getMessage());
            }
            return active.snapshot();
        }

        try {
            active = decode(Files.readString(file, StandardCharsets.UTF_8)).requireValid();
        } catch (IOException e) {
            warningSink.accept("Could not read " + file + ": " + e.getMessage());
        } catch (IllegalArgumentException | JsonParseException e) {
            warningSink.accept("Invalid configuration in " + file + ": " + e.getMessage());
            try {
                var backup = backupInvalidFile();
                writeAtomically(active);
                warningSink.accept("Preserved the invalid configuration at " + backup);
            } catch (IOException backupError) {
                warningSink.accept("Could not back up or replace " + file + ": " + backupError.getMessage());
            }
        }
        return active.snapshot();
    }

    public synchronized BetterAnvilsConfig current() {
        return active.snapshot();
    }

    public synchronized void save(BetterAnvilsConfig draft) throws IOException {
        var validated = Objects.requireNonNull(draft).requireValid().snapshot();
        try {
            writeAtomically(validated);
            active = validated;
        } catch (IOException e) {
            warningSink.accept("Could not save " + file + ": " + e.getMessage());
            throw e;
        }
    }

    private Path backupInvalidFile() throws IOException {
        var suffix = Instant.now().toString().replace(':', '-');
        var backup = file.resolveSibling(file.getFileName() + ".invalid-" + suffix + ".bak");
        return Files.copy(file, backup);
    }

    private void writeAtomically(BetterAnvilsConfig config) throws IOException {
        var parent = file.getParent();
        Files.createDirectories(parent);
        var temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, encode(config), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String encode(BetterAnvilsConfig config) {
        var json = new JsonObject();
        json.addProperty("maximumAnvilCost", config.maximumAnvilCost());
        json.addProperty("anvilDiscountPercent", config.anvilDiscountPercent());
        json.addProperty("minimumAnvilCost", config.minimumAnvilCost());
        json.addProperty("preservePriorWorkPenalty", config.preservePriorWorkPenalty());
        return GSON.toJson(json) + System.lineSeparator();
    }

    private static BetterAnvilsConfig decode(String json) {
        var root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("configuration root must be an object");
        }
        var object = root.getAsJsonObject();
        var config = new BetterAnvilsConfig(
                integer(object, "maximumAnvilCost"),
                integer(object, "anvilDiscountPercent"),
                integer(object, "minimumAnvilCost"),
                bool(object, "preservePriorWorkPenalty")
        );
        return config.requireValid();
    }

    private static int integer(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(field + " must be a JSON integer");
        }
        try {
            return new BigDecimal(value.getAsString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException(field + " must be a JSON integer", e);
        }
    }

    private static boolean bool(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(field + " must be a JSON boolean");
        }
        return value.getAsBoolean();
    }
}
