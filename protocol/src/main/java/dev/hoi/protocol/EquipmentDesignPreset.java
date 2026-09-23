package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record EquipmentDesignPreset(String chassis,String name,Map<String,String> modules,Map<String,Integer> upgrades) {
    private static final Gson JSON=new Gson();
    public EquipmentDesignPreset {
        IndustryView.text(Objects.requireNonNull(chassis),128);IndustryView.text(Objects.requireNonNull(name),80);
        if(chassis.isBlank()||name.isBlank()||modules==null||upgrades==null||modules.size()>16||upgrades.size()>2)
            throw new IllegalArgumentException("Invalid design preset");
        modules=Map.copyOf(modules);upgrades=Map.copyOf(upgrades);
        modules.forEach((slot,part)->{IndustryView.text(slot,128);IndustryView.text(part,128);if(slot.isBlank()||part.isBlank())throw new IllegalArgumentException("Invalid preset module");});
        upgrades.forEach((id,level)->{IndustryView.text(id,128);if(id.isBlank()||level<0||level>20)throw new IllegalArgumentException("Invalid preset upgrade");});
    }
    public static EquipmentDesignPreset from(TankDesignerView view,String name) {
        var modules=new TreeMap<String,String>();view.slots().forEach(s->{if(!s.selected().isEmpty())modules.put(s.id(),s.selected());});
        var upgrades=new TreeMap<String,Integer>();view.upgrades().forEach(u->upgrades.put(u.id(),u.level()));
        return new EquipmentDesignPreset(view.chassis(),name,modules,upgrades);
    }
    public String json(){return JSON.toJson(this);}
    public static EquipmentDesignPreset parse(String json){
        IndustryView.text(json,8192);
        try{return ProtocolJson.read(JSON,json,EquipmentDesignPreset.class);}
        catch(RuntimeException error){throw new IllegalArgumentException("Invalid design preset",error);}
    }
}
