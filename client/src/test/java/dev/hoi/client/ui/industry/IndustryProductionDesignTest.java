package dev.hoi.client.ui.industry;

import dev.hoi.protocol.industry.IndustryView;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IndustryProductionDesignTest {
    private IndustryView.Equipment equipment(String id,String group) {
        return new IndustryView.Equipment(id,"장비","",group,group.equals("navy"),true,1,10,Map.of(),0,0,0,0);
    }
    @Test void ordinaryArmorAndModularTankUseDifferentSourceFrames() {
        var ordinary=IndustryEquipmentDesigns.productionDesign(equipment("hoi:atgm","armor"));
        var tank=IndustryEquipmentDesigns.productionDesign(equipment("hoi:chassis/modern_tank_chassis_1","armor"));
        assertEquals("production/picker/variant_armor",ordinary.icon());
        assertFalse(ordinary.editable());
        assertEquals("production/picker/variant_tank",tank.icon());
        assertTrue(tank.editable());
        assertTrue(tank.unfinished());
    }
    @Test void savedDesignsAndUnsupportedBlueprintsHaveDifferentActions() {
        for(String group:java.util.List.of("armor","navy","air")) {
            var saved=IndustryEquipmentDesigns.productionDesign(equipment("hoi:design/test",group));
            var unsupported=IndustryEquipmentDesigns.productionDesign(equipment("hoi:blueprint/test",group));
            assertTrue(saved.editable());
            assertFalse(saved.unfinished());
            assertTrue(unsupported.blueprint());
            assertFalse(unsupported.editable());
            assertEquals(saved.icon(),unsupported.icon());
        }
        assertEquals("production/picker/variant",IndustryEquipmentDesigns.productionDesign(equipment("hoi:gun","infantry")).icon());
    }
}
