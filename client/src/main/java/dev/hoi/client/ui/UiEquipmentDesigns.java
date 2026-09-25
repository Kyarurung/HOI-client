package dev.hoi.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.protocol.TankDesignerView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.List;

public final class UiEquipmentDesigns {
    private static JsonObject details;
    private static final java.util.Map<TankDesignerView.Stat,String> STAT_TOOLTIPS = new java.util.HashMap<>();
    private static final java.util.Map<String,List<TankDesignerView.Stat>> MODULE_STATS = new java.util.HashMap<>();
    private static final java.util.Map<String,String> TOOLTIPS = new java.util.HashMap<>();
    private UiEquipmentDesigns() {}
    public static void clear() {details=null;MODULE_STATS.clear();TOOLTIPS.clear();STAT_TOOLTIPS.clear();}
    private static JsonObject details() {
        if(details!=null)return details;
        details=new JsonObject();
        var resource=Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:ui/equipment_design_details.json"));
        if(resource.isPresent())try(var input=resource.get().open()) {
            var bytes=input.readNBytes(2_000_001);
            if(bytes.length<=2_000_000)details=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(java.io.IOException|RuntimeException ignored) {}
        return details;
    }
    private static JsonObject object(JsonObject parent,String key) {
        return parent.has(key)&&parent.get(key).isJsonObject()?parent.getAsJsonObject(key):new JsonObject();
    }
    public record ProductionDesign(String icon, String title, boolean blueprint, boolean unfinished, boolean editable) {}
    public static ProductionDesign productionDesign(dev.hoi.protocol.IndustryView.Equipment equipment) {
        boolean blueprint=equipment.id().startsWith("hoi:blueprint/");
        boolean chassis=equipment.id().startsWith("hoi:chassis/");
        boolean saved=equipment.id().startsWith("hoi:design/");
        boolean modular=blueprint||chassis||saved;
        String suffix=switch(equipment.group()) {
            case "armor" -> modular?"_tank":"_armor";
            case "navy" -> "_navy";
            case "air" -> "_air";
            default -> "";
        };
        String title=!modular?"파생형 제작":switch(equipment.group()) {
            case "armor" -> "전차 설계사";
            case "navy" -> "함선 설계사";
            case "air" -> "항공기 설계사";
            default -> "파생형 제작";
        };
        return new ProductionDesign("production/picker/variant"+suffix,title,blueprint,blueprint||chassis,chassis||saved);
    }
    public static String productionObsoleteTooltip(boolean outdated) {
        String key="PRODUCTION_VARIANT_OBSOLETE_"+(outdated?"YES":"NO");
        return UiTooltipText.sections(tooltip(key),tooltip(key+"_DESC"));
    }
    public static String role(TankDesignerView design) {
        String selected=design.slots().stream().filter(s->s.id().equals("fixed_main_weapon_slot")).map(TankDesignerView.Slot::selected).findFirst().orElse("");
        var module=object(object(details(),"modules"),selected);
        return module.has("role")?module.get("role").getAsString():"";
    }
    public static String roleName(String role) {
        var names=object(details(),"roles");
        return names.has(role)?names.get(role).getAsString():"역할 배정을 위해 주무장을 추가하십시오.";
    }

    public static String adjusterName(String key) {
        var names=object(details(),"adjusterNames");
        return names.has(key)?names.get(key).getAsString():key;
    }
    public static String statName(String key) {
        var names=object(details(),"stats");
        return names.has(key)?names.get(key).getAsString():key;
    }
    public static String statCaption(String key,String fallback) {
        var icon=object(object(details(),"statIcons"),key);
        String name=statName(key);
        if(name.equals(key))name=fallback.replaceAll("£[A-Za-z0-9_]+\\s*","");
        return (icon.has("token")?"£"+icon.get("token").getAsString()+" ":"")+name;
    }
    public static String tooltip(String key) {
        return TOOLTIPS.computeIfAbsent(key,id->{
            var text=object(details(),"tooltips");
            return sourceText(text.has(id)?text.get(id).getAsString():id);
        });
    }
    public static String sourceText(String text) {
        return text.replace("§H","§e").replace("§Y","§e").replace("§G","§a").replace("§R","§c").replace("§W","§f").replace("§!","§f").replace("§T","§f");
    }
    public static String statDescription(String id) {
        var descriptions=object(details(),"statDescriptions");
        return descriptions.has(id)?sourceText(descriptions.get(id).getAsString()):"";
    }
    public static boolean beneficial(String id,double change) {
        var formats=object(details(),"statDiffFormats");
        return formats.has(id)&&formats.get(id).getAsString().contains("|=-")?change<0:change>=0;
    }
    public static String statValue(TankDesignerView.Stat stat,double value) {
        if(stat.id().equals("carrier_size"))return Long.toString((long)value);
        int decimals=java.util.Set.of("supply_consumption","fuel_consumption","build_cost_ic").contains(stat.id())?2:1;
        return java.math.BigDecimal.valueOf(value).multiply(java.math.BigDecimal.valueOf(stat.percent()?100:1))
                .setScale(8,java.math.RoundingMode.HALF_UP).setScale(decimals,java.math.RoundingMode.DOWN).toPlainString()
                +(stat.percent()?"%":stat.id().equals("maximum_speed")?"km/h":stat.id().equals("naval_speed")?"kn":java.util.Set.of("air_range","naval_range").contains(stat.id())?"km":"");
    }
    public static String signedValue(TankDesignerView.Stat stat,double value) {
        return (beneficial(stat.id(),value)?"§a":"§c")+(value>=0?"+":"")+statValue(stat,value)+"§f";
    }
    public static String statTooltip(TankDesignerView.Stat stat) {
        var cached=STAT_TOOLTIPS.get(stat);
        if(cached!=null)return cached;
        if(STAT_TOOLTIPS.size()>=64)STAT_TOOLTIPS.clear();
        String result=buildStatTooltip(stat);
        STAT_TOOLTIPS.put(stat,result);
        return result;
    }
    private static String buildStatTooltip(TankDesignerView.Stat stat) {
        String title=stat.id().equals("build_cost_ic")?tooltip("EQUIPMENT_DESIGNER_PRODUCTION_COST_DESC"):
                "§e"+statCaption(stat.id(),stat.name()).strip().replaceAll(":$","")+"§f\n"+statDescription(stat.id());
        String sources=stat.contributions().stream().map(source->"§f"+source.name()+": "+signedValue(stat,source.value())).collect(java.util.stream.Collectors.joining("\n"));
        String changed=Math.abs(stat.change())<1e-8||stat.id().equals("build_cost_ic")?"":
                "§f"+tooltip("DESIGNER_UNSAVED_CHANGES")+" "+signedValue(stat,stat.change())
                +(beneficial(stat.id(),stat.change())?"§a":"§c")+(stat.change()>0?"▲":"▼");
        return UiTooltipText.sections(title,sources,changed);
    }
    public static String moduleDescription(String id) {
        var module=object(object(details(),"modules"),id);
        return sourceText(module.has("description")?module.get("description").getAsString():"");
    }
    public static java.util.List<TankDesignerView.Stat> moduleStats(String id) {
        return MODULE_STATS.computeIfAbsent(id,UiEquipmentDesigns::readModuleStats);
    }
    private static java.util.List<TankDesignerView.Stat> readModuleStats(String id) {
        var module=object(object(details(),"modules"),id);
        var result=new java.util.ArrayList<TankDesignerView.Stat>();
        for(String group:java.util.List.of("add","multiply"))for(var entry:object(module,group).entrySet()) {
            double value=entry.getValue().getAsDouble();
            if(value!=0)result.add(new TankDesignerView.Stat(entry.getKey(),statName(entry.getKey()),value,group.equals("multiply")||java.util.Set.of("reliability","hardness").contains(entry.getKey())));
        }
        var order=List.of("soft_attack","hard_attack","air_attack","defense","breakthrough","maximum_speed","armor_value","ap_attack","hardness","reliability","fuel_consumption","build_cost_ic");
        result.sort(java.util.Comparator.comparingInt(stat->order.contains(stat.id())?order.indexOf(stat.id()):order.size()));
        return java.util.List.copyOf(result);
    }
    public static String moduleCategory(String id) {
        var module=object(object(details(),"modules"),id);
        return module.has("category")?module.get("category").getAsString():"";
    }
    public static List<String> categoryIds(String chassis,String slot) {
        var slots=object(object(details(),"slotCategoryIds"),chassis);
        if(!slots.has(slot))return List.of();
        return slots.getAsJsonArray(slot).asList().stream().map(com.google.gson.JsonElement::getAsString).toList();
    }
    public static String categoryName(String id) {
        var category=object(object(details(),"categories"),id);
        return category.has("name")?sourceText(category.get("name").getAsString()):id;
    }
    public static String categoryTexture(String id) {
        var category=object(object(details(),"categories"),id);
        return category.has("texture")?category.get("texture").getAsString():"";
    }
    public static java.util.Map<String,Double> moduleResources(String id) {
        var resources=object(object(object(details(),"modules"),id),"resources");
        var result=new java.util.LinkedHashMap<String,Double>();
        resources.entrySet().forEach(entry->result.put(entry.getKey(),entry.getValue().getAsDouble()));
        return result;
    }
    public static int moduleExperience(String id) {
        var module=object(object(details(),"modules"),id);
        return module.has("xp")?module.get("xp").getAsInt():0;
    }
    public static String slotCategories(String chassis,String slot) {
        var slots=object(object(details(),"slots"),chassis);
        if(!slots.has(slot))return "";
        var labels=new java.util.ArrayList<String>();
        for(var label:slots.getAsJsonArray(slot))labels.add(sourceText(label.getAsString()));
        return String.join("\n",labels);
    }
    public static void overlays(GuiGraphicsExtractor g,TankDesignerView design,String hovered,int x,int y,int width,int height) {
        var slots=object(object(object(details(),"chassis"),design.chassis()),"slots");
        var modules=object(details(),"modules");
        float scale=Math.min(width/512f,height/260f);
        int left=x+Math.round((width-512*scale)/2),top=y+Math.round((height-260*scale)/2);
        g.enableScissor(x,y,x+width,y+height);
        for(var slot:design.slots()) {
            var choices=object(slots,slot.id());var module=object(modules,slot.selected());
            String category=module.has("category")?module.get("category").getAsString():"";
            String selected=choices.has(slot.selected())?slot.selected():category;
            for(String key:List.of(selected,slot.id().equals(hovered)?selected+"@highlight":"")) {
                if(!choices.has(key))continue;
                for(var item:choices.getAsJsonArray(key)) {
                    var layer=item.getAsJsonObject();
                    UiAssets.draw(g,layer.get("texture").getAsString(),left+Math.round(layer.get("x").getAsInt()*scale),top+Math.round(layer.get("y").getAsInt()*scale),
                            Math.round(layer.get("width").getAsInt()*scale),Math.round(layer.get("height").getAsInt()*scale));
                }
            }
        }
        g.disableScissor();
    }
}
