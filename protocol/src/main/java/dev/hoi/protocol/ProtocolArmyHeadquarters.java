package dev.hoi.protocol;

import java.util.*;
import static dev.hoi.protocol.ProtocolIndustryView.*;

public record ProtocolArmyHeadquarters(List<Template> templates,Template draft,boolean copy,boolean dirty,String saveReason,int armies,Map<String,String> labels,List<Slot> slots,Details details) {
    public ProtocolArmyHeadquarters(List<Template> templates,Template draft,boolean copy,boolean dirty,String saveReason,int armies,Map<String,String> labels,List<Slot> slots){this(templates,draft,copy,dirty,saveReason,armies,labels,slots,null);}
    public ProtocolArmyHeadquarters {slots=slots==null?List.of():bounded(slots,8);labels=labels==null?Map.of():Map.copyOf(labels);if(labels.size()>128)throw new IllegalArgumentException("HQ labels too large");labels.forEach((key,value)->{text(key,128);text(value,256);});templates=bounded(templates,128);text(saveReason,2048);if(armies<0)throw new IllegalArgumentException("Invalid HQ army count");}
    private static <T> List<T> bounded(List<T> values,int max){if(values==null||values.size()>max)throw new IllegalArgumentException("HQ view too large");return List.copyOf(values);}
    public record Details(Map<String,String> units,String leaderBenefits,double baseHitpoints,List<ProtocolTankDesigner.Adjustment> adjustments,List<ProtocolTankDesigner.Stat> stats){public Details(Map<String,String> units,String leaderBenefits,double baseHitpoints,List<ProtocolTankDesigner.Adjustment> adjustments){this(units,leaderBenefits,baseHitpoints,adjustments,List.of());}public Details(Map<String,String> units,String leaderBenefits,double baseHitpoints){this(units,leaderBenefits,baseHitpoints,List.of());}public Details{stats=stats==null?List.of():bounded(stats,32);adjustments=adjustments==null?List.of():bounded(adjustments,11);if(new HashSet<>(adjustments.stream().map(ProtocolTankDesigner.Adjustment::id).toList()).size()!=adjustments.size())throw new IllegalArgumentException("Duplicate HQ terrain");units=Map.copyOf(units);if(units.size()>32||!Double.isFinite(baseHitpoints)||baseHitpoints<0)throw new IllegalArgumentException("Invalid HQ details");units.forEach((key,value)->{text(key,80);text(value,3000);});text(leaderBenefits,3000);}}
    public record Choice(String id,String reason,long manpower,Map<String,Long> equipment,double baseCommandPower,List<ProtocolTankDesigner.Stat> changes) {
        public Choice(String id,String reason,long manpower,Map<String,Long> equipment,double baseCommandPower){this(id,reason,manpower,equipment,baseCommandPower,List.of());}
        public Choice {changes=changes==null?List.of():bounded(changes,32);text(id,80);text(reason,512);equipment=Map.copyOf(equipment);if(equipment.size()>32||equipment.values().stream().anyMatch(v->v<0)||manpower<0||!Double.isFinite(baseCommandPower)||baseCommandPower<0)throw new IllegalArgumentException("Invalid HQ candidate");equipment.keySet().forEach(v->text(v,128));}
    }
    public record Slot(boolean support,int index,String current,String reason,List<Choice> choices) {
        public Slot {if(index<0||index>=4)throw new IllegalArgumentException("Invalid HQ slot");text(current,80);text(reason,512);choices=bounded(choices,32);}
    }
    public record Template(String id,String name,long version,List<String> battalions,List<String> support,long manpower,Map<String,Long> equipment,double baseCommandPower) {
        public Template {text(id,128);text(name,80);battalions=bounded(battalions,4);support=bounded(support,4);battalions.forEach(v->text(v,80));support.forEach(v->text(v,80));equipment=Map.copyOf(equipment);if(equipment.size()>32||equipment.values().stream().anyMatch(v->v<0)||version<1||manpower<0||!Double.isFinite(baseCommandPower)||baseCommandPower<0)throw new IllegalArgumentException("Invalid HQ template view");equipment.keySet().forEach(v->text(v,128));}
    }
}
