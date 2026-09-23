package dev.hoi.protocol;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentDesignPresetTest {
    @Test void presetsRoundTripAndRejectMalformedOrUnboundedInput() {
        var preset=new EquipmentDesignPreset("modern_tank_chassis_1","시험 전차",Map.of("main_armament_slot","tank_heavy_cannon"),Map.of("tank_engine_upgrade",3));
        assertEquals(preset,EquipmentDesignPreset.parse(preset.json()));
        for(String invalid:new String[]{"null","{}","{","{\"chassis\":\"x\",\"name\":\"x\",\"modules\":{},\"upgrades\":{\"engine\":21}}"})
            assertThrows(IllegalArgumentException.class,()->EquipmentDesignPreset.parse(invalid));
        assertThrows(IllegalArgumentException.class,()->EquipmentDesignPreset.parse("x".repeat(8193)));
        assertThrows(IllegalArgumentException.class,()->new IndustryProtocol.Request(IndustryProtocol.Action.TANK_MODULE,"",0,"slot","x".repeat(129),0));
    }
}
