package dev.hoi.client.ui;

import dev.hoi.protocol.EquipmentDesignPreset;
import java.nio.file.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentDesignPresetsTest {
    @TempDir Path directory;
    @Test void reloadsAcrossInstancesAndReplacesOnlyMatchingChassisAndName() throws Exception {
        Path path=directory.resolve("presets.json");var store=new EquipmentDesignPresets(path);
        var tank=new EquipmentDesignPreset("tank","공통 이름",Map.of("gun","one"),Map.of());
        var plane=new EquipmentDesignPreset("plane","공통 이름",Map.of(),Map.of());
        store.save(tank);store.save(plane);
        var revised=new EquipmentDesignPreset("tank","공통 이름",Map.of("gun","two"),Map.of());store.save(revised);
        var loaded=new EquipmentDesignPresets(path).load();
        assertEquals(2,loaded.size());assertTrue(loaded.contains(plane));assertTrue(loaded.contains(revised));
    }
    @Test void corruptFilesArePreservedWhenSaveFails() throws Exception {
        Path path=directory.resolve("presets.json");Files.writeString(path,"broken original");
        var store=new EquipmentDesignPresets(path);
        assertThrows(java.io.IOException.class,()->store.save(new EquipmentDesignPreset("tank","이름",Map.of(),Map.of())));
        assertEquals("broken original",Files.readString(path));
    }
}
