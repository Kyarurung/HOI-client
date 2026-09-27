package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolIndustryView.Equipment;
import java.util.*;

final class ScreenProductionIndicators {
    private ScreenProductionIndicators() {}
    record Demand(Map<String,Integer> targets,Map<String,Long> shortages) {}
    static Demand demand(List<Equipment> equipment) {
        var demand=new HashMap<String,Long>();
        var stock=new HashMap<String,Long>();
        var upgrades=new HashSet<String>();
        var unknown=new HashSet<String>();
        for(var item:equipment) {
            String family=family(item);
            if(item.demand()!=null)demand.merge(family,item.demand(),Math::addExact);
            else unknown.add(family);
            stock.merge(family,item.stockpile(),Math::addExact);
            if(item.deployed()>0&&item.replacement()!=null&&!item.replacement().isEmpty())upgrades.add(item.replacement());
        }
        var result=new HashMap<String,Integer>();
        var shortages=new HashMap<String,Long>();
        for(var item:equipment) {
            String family=family(item);
            if(unknown.contains(family))continue;
            long shortage=Math.max(0,demand.getOrDefault(family,0L)-stock.getOrDefault(family,0L));
            shortages.put(item.id(),shortage);
            if(!item.naval())result.put(item.id(),shortage>0?2:upgrades.contains(item.id())?1:0);
        }
        return new Demand(Map.copyOf(result),Map.copyOf(shortages));
    }
    private static String family(Equipment item) {
        return item.group()+"/"+Objects.toString(item.family(),item.id());
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
