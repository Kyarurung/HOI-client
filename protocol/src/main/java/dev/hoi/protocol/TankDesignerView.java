package dev.hoi.protocol;

import java.util.*;

public record TankDesignerView(String chassis,String name,String texture,List<Slot> slots,List<Stat> stats,
        List<Upgrade> upgrades,Map<String,Double> resources,double cost,int experience,List<String> errors,List<Mission> missions,List<Adjustment> adjustments,String family) {
    public TankDesignerView(String chassis,String name,String texture,List<Slot> slots,List<Stat> stats,
            List<Upgrade> upgrades,Map<String,Double> resources,double cost,int experience,List<String> errors,List<Mission> missions,List<Adjustment> adjustments) {
        this(chassis,name,texture,slots,stats,upgrades,resources,cost,experience,errors,missions,adjustments,"");
    }
    public TankDesignerView(String chassis,String name,String texture,List<Slot> slots,List<Stat> stats,
            List<Upgrade> upgrades,Map<String,Double> resources,double cost,int experience,List<String> errors,List<Mission> missions) {
        this(chassis,name,texture,slots,stats,upgrades,resources,cost,experience,errors,missions,List.of());
    }
    public TankDesignerView(String chassis,String name,String texture,List<Slot> slots,List<Stat> stats,
            List<Upgrade> upgrades,Map<String,Double> resources,double cost,int experience,List<String> errors) {
        this(chassis,name,texture,slots,stats,upgrades,resources,cost,experience,errors,List.of());
    }
    public record Mission(String id,String name,List<Stat> stats) {
        public Mission {IndustryView.text(id,64);IndustryView.text(name,128);stats=List.copyOf(stats);if(stats.size()>64)throw new IllegalArgumentException("Too many mission stats");}
    }
    public record Adjustment(String id,double movement,double attack,double defense) {
        public Adjustment {IndustryView.text(id,64);if(!Double.isFinite(movement)||!Double.isFinite(attack)||!Double.isFinite(defense))throw new IllegalArgumentException("Invalid adjustment");}
    }
    public record Part(String id,String name,String texture,int experience,List<Stat> changes) {
        public Part {IndustryView.text(id,128);IndustryView.text(name,128);IndustryView.text(texture,160);changes=List.copyOf(changes);if(changes.size()>32||experience<0)throw new IllegalArgumentException("Invalid module");}
    }
    public record Slot(String id,String name,boolean required,String selected,List<Part> choices) {
        public Slot {IndustryView.text(id,128);IndustryView.text(name,128);IndustryView.text(selected,128);choices=List.copyOf(choices);if(choices.size()>128)throw new IllegalArgumentException("Too many modules");}
    }
    public record Stat(String id,String name,double value,boolean percent) {
        public Stat {IndustryView.text(id,64);IndustryView.text(name,128);if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid design stat");}
    }
    public record Upgrade(String id,String name,int level,int maximum,List<Stat> increase,List<Stat> decrease,List<String> requirements,
            List<Stat> perLevel,Map<String,Double> resourceIncrease,Map<String,Double> resourceDecrease) {
        public Upgrade {IndustryView.text(id,64);IndustryView.text(name,128);increase=List.copyOf(increase);decrease=List.copyOf(decrease);requirements=List.copyOf(requirements);perLevel=perLevel==null?increase:List.copyOf(perLevel);resourceIncrease=resourceIncrease==null?Map.of():Map.copyOf(resourceIncrease);resourceDecrease=resourceDecrease==null?Map.of():Map.copyOf(resourceDecrease);if(level<0||level>20||maximum<0||maximum>20||perLevel.size()>32||resourceIncrease.size()>8||resourceDecrease.size()>8)throw new IllegalArgumentException("Invalid design level");}
    }
    public TankDesignerView {
        IndustryView.text(chassis,128);IndustryView.text(name,128);IndustryView.text(texture,160);
        slots=List.copyOf(slots);stats=List.copyOf(stats);upgrades=List.copyOf(upgrades);resources=Map.copyOf(resources);errors=List.copyOf(errors);
        missions=missions==null?List.of():List.copyOf(missions);
        adjustments=adjustments==null?List.of():List.copyOf(adjustments);
        family=family==null?"":family;IndustryView.text(family,64);
        if(slots.size()>16||stats.size()>64||missions.size()>16||adjustments.size()>16||upgrades.size()>2||resources.size()>8||errors.size()>32||!Double.isFinite(cost)||cost<=0||experience<0)
            throw new IllegalArgumentException("Invalid tank designer snapshot");
    }
}
