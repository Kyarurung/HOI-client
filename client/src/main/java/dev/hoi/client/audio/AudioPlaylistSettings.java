package dev.hoi.client.audio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

final class AudioPlaylistSettings {
    private final Path file;
    private final Properties properties = new Properties();
    private double value = 1.0;
    private boolean station;
    AudioPlaylistSettings(Path file) {
        this.file = file;
    }
    double value() {
        return value;
    }
    boolean station() {
        return station;
    }
    void load() throws IOException {
        if (!Files.exists(file)) return;
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        try {
            double saved = Double.parseDouble(properties.getProperty("playlistVolume", "1.0"));
            if (Double.isFinite(saved)) value = Math.clamp(saved, 0.0, 1.0);
        } catch (NumberFormatException ignored) {}
        station = Boolean.parseBoolean(properties.getProperty("koreanStation", "false"));
    }
    void set(double requested) throws IOException {
        if (!Double.isFinite(requested)) return;
        double next = Math.clamp(requested, 0.0, 1.0);
        if (next == value && Files.exists(file)) return;
        value = next;
        properties.setProperty("playlistVolume", Double.toString(value));
        save();
    }
    void station(boolean enabled) throws IOException {
        if (enabled == station && Files.exists(file)) return;
        station = enabled;
        properties.setProperty("koreanStation", Boolean.toString(enabled));
        save();
    }
    private void save() throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        var temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "hoi-audio-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, null);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
