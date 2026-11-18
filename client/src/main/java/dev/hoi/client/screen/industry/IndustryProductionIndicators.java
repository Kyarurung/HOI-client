package dev.hoi.client.screen.industry;

import dev.hoi.protocol.industry.IndustryView.Equipment;
import java.util.*;

final class IndustryProductionIndicators {
    private IndustryProductionIndicators() {}
    static String navalDays(double daily,double progress) {
        return daily>0?(long)Math.ceil(Math.max(0,1-progress)/daily)+"일":"생산량 없음";
    }
    static String targetKey(int target) {
        return "PROD_LINE_LAND_DEST_"+switch(target) {case 1->"UPGRADING";case 2->"REINFORCEMENT";default->"RESERVES";};
    }
    static String landRole(Equipment equipment) {
        if(equipment.naval()||equipment.group().equals("air")||equipment.id().startsWith("hoi:chassis/"))return "";
        String role=equipment.designer()==null?Objects.toString(equipment.sourceEquipment(),""):equipment.designer().role();
        int frame=role.contains("anti_air")||role.contains("_aa_")||role.contains("spaa")?2:
                role.contains("tank_destroyer")||role.contains("anti_tank")?0:
                role.contains("rocket")||role.contains("artillery")||role.contains("spg")?1:-1;
        return frame<0?"":"production/roles/land_"+frame;
    }
}
