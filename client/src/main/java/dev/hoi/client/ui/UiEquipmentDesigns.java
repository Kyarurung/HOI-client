package dev.hoi.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.protocol.ProtocolTankDesigner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.List;

public final class UiEquipmentDesigns {
    private static JsonObject details;
    private static final java.util.Map<ProtocolTankDesigner.Stat,String> STAT_TOOLTIPS = new java.util.HashMap<>();
    private static final java.util.Map<String,List<ProtocolTankDesigner.Stat>> MODULE_STATS = new java.util.HashMap<>();
    private static final java.util.Map<String,String> TOOLTIPS = new java.util.HashMap<>();
    private UiEquipmentDesigns() {}
    public static void clear() {details=null;MODULE_STATS.clear();TOOLTIPS.clear();STAT_TOOLTIPS.clear();}
    private static JsonObject details() {
        if(details!=null)return details;
        details=new JsonObject();
        var resource=Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:ui/equipment_design_details.json"));
        if(resource.isPresent())try(var input=resource.get().open()) {
            var bytes=input.readNBytes(4_000_001);
            if(bytes.length<=4_000_000)details=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(java.io.IOException|RuntimeException ignored) {}
        return details;
    }
    private static JsonObject object(JsonObject parent,String key) {
        return parent.has(key)&&parent.get(key).isJsonObject()?parent.getAsJsonObject(key):new JsonObject();
    }
    public record ProductionDesign(String icon, String title, boolean blueprint, boolean unfinished, boolean editable) {}
    public static ProductionDesign productionDesign(dev.hoi.protocol.ProtocolIndustryView.Equipment equipment) {
        boolean blueprint=equipment.id().startsWith("hoi:blueprint/");
        boolean chassis=equipment.id().startsWith("hoi:chassis/");
        boolean saved=equipment.id().startsWith("hoi:design/");
        boolean modular=blueprint||chassis||saved||equipment.designer()!=null;
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
        return new ProductionDesign("production/picker/variant"+suffix,title,blueprint,blueprint||chassis,chassis||saved||equipment.designer()!=null);
    }
    public static String productionRoleIcon(dev.hoi.protocol.ProtocolIndustryView.Equipment equipment) {
        String role=equipment.designer()==null?java.util.Objects.toString(equipment.family(),""):equipment.designer().role();
        if(equipment.group().equals("air")) {
            role=switch(role){case "transport_plane"->"transport";case "tfr_attack_helicopter"->"attack_helicopter";default->role;};
            return java.util.Set.of("fighter","heavy_fighter","cas","naval_bomber","tactical_bomber","strategic_bomber","transport","scout_plane","scout","carrier_fighter","carrier_bomber","attack_helicopter").contains(role)?"production/design/roles/"+role:"";
        }
        return "";
    }
    public static String roleTooltip(String role) {
        return role.isEmpty()?tooltip("EQUIPMENT_DESIGNER_PLANE_ROLE_REQUIRE_WEAPON"):"§e"+roleName(role);
    }
    public static String categoryTooltip(String id) {
        var category=object(object(details(),"categories"),id);
        return UiTooltipText.sections("§e"+categoryName(id),category.has("description")?sourceText(category.get("description").getAsString()):"");
    }
    private static String missionName(String id) {
        var mission=object(object(details(),"missions"),id);
        return mission.has("name")?sourceText(mission.get("name").getAsString()):id;
    }
    public static String equipmentDescription(String country,String source) {
        if(source==null)return "";
        var descriptions=object(details(),"equipmentDescriptions");
        String key=descriptions.has(country+"_"+source)?country+"_"+source:source;
        return descriptions.has(key)?sourceText(descriptions.get(key).getAsString()):"";
    }
    public static String productionMissionTooltip(String id) {
        return "§e"+missionName(id);
    }
    public static String productionMissionIcon(String id) {
        int frame=switch(id) {
            case "air_superiority"->0;case "cas"->1;case "interception"->2;case "strategic_bomber"->3;
            case "naval_bomber"->4;case "air_supply"->7;case "port_strike"->8;
            case "naval_mines_planting"->9;case "naval_mines_sweeping"->10;case "recon"->12;
            case "attack_logistics"->13;case "naval_patrol"->14;case "kamikaze"->15;default->-1;
        };
        return frame<0?"":"air_detail/mission_"+frame;
    }
    private static String missionEffects(JsonObject rule) {
        var lines=new java.util.ArrayList<String>();
        for(String group:List.of("add","average","multiply"))for(var value:object(rule,group).entrySet()) {
            double amount=value.getValue().getAsDouble();
            if(amount==0)continue;
            lines.add("§f"+statCaption(value.getKey(),statName(value.getKey()))+" "+(beneficial(value.getKey(),amount)?"§a":"§c")+(amount>0?"+":"")+number(amount*(group.equals("multiply")?100:1),1)+(group.equals("multiply")?"%":"")+"§f"+(group.equals("average")?averageLabel():""));
        }
        return String.join("\n",lines);
    }
    public static String missionTooltip(ProtocolTankDesigner design,String id) {
        var lines=new java.util.ArrayList<String>();
        lines.add("§e"+design.missions().stream().filter(m->m.id().equals(id)).map(ProtocolTankDesigner.Mission::name).findFirst().orElseGet(()->missionName(id)));
        design.adjustments().stream().filter(a->a.id().equals(id)).findFirst().ifPresent(adjustment->{
            for(var stat:adjustment.effects()) {
                double amount=stat.value();
                lines.add("§f"+statCaption(stat.id(),stat.name())+" "+(beneficial(stat.id(),amount)?"§a":"§c")+(amount>0?"+":"")+number(amount*(stat.percent()?100:1),1)+(stat.percent()?"%":"")+"§f"+(stat.averaged()?averageLabel():""));
            }
        });
        return String.join("\n",lines);
    }
    private static String averageLabel() {
        return " §f("+tooltip("EQUIPMENT_MODULE_STAT_AVERAGED").strip().replace("(","").replace(")","")+")";
    }
    private static String moduleMissions(JsonObject module) {
        var sections=new java.util.ArrayList<String>();
        if(module.has("missions"))for(var mission:module.getAsJsonArray("missions"))sections.add(tooltip("MODULE_ALLOW_MISSION").replace("$MISSION|H$","§e"+missionName(mission.getAsString())+"§f"));
        if(module.has("missionStats"))for(var value:module.getAsJsonArray("missionStats")) {
            var rule=value.getAsJsonObject();
            String names=rule.getAsJsonArray("missions").asList().stream().map(m->"§e"+missionName(m.getAsString())+"§f").collect(java.util.stream.Collectors.joining(", "));
            sections.add(tooltip("MODULE_MISSION_RESTRICTED_STATS_HEADER").replace("$MISSIONS$",names)+"\n"+missionEffects(rule));
        }
        return UiTooltipText.sections(sections.toArray(String[]::new));
    }
    public static String productionObsoleteTooltip(boolean outdated) {
        String key="PRODUCTION_VARIANT_OBSOLETE_"+(outdated?"YES":"NO");
        return UiTooltipText.sections(tooltip(key),tooltip(key+"_DESC"));
    }
    public static String role(ProtocolTankDesigner design) {
        String selected=design.slots().stream().filter(s->s.id().equals("fixed_main_weapon_slot")).map(ProtocolTankDesigner.Slot::selected).findFirst().orElse("");
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
    private static String number(double value,int decimals) {
        return java.math.BigDecimal.valueOf(value).setScale(8,java.math.RoundingMode.HALF_UP).setScale(decimals,java.math.RoundingMode.DOWN).toPlainString();
    }
    public static String statValue(ProtocolTankDesigner.Stat stat,double value) {
        var formats=object(details(),"statValueFormats");
        String format=formats.has(stat.id())?formats.get(stat.id()).getAsString():"";
        var precision=java.util.regex.Pattern.compile("\\|[^$]*?([0-9])").matcher(format);
        int decimals=precision.find()?Integer.parseInt(precision.group(1)):1;
        String unit=stat.percent()?"%":stat.id().equals("maximum_speed")?"km/h":stat.id().equals("naval_speed")?"kn":java.util.Set.of("air_range","naval_range").contains(stat.id())?"km":"";
        return number(value*(stat.percent()?100:1),decimals)+unit;
    }
    public static String signedValue(ProtocolTankDesigner.Stat stat,double value) {
        int decimals=java.util.Set.of("supply_consumption","fuel_consumption","build_cost_ic").contains(stat.id())?2:1;
        boolean weather=stat.id().equals("naval_weather_penalty_factor");
        return (weather?"§e":beneficial(stat.id(),value)?"§a":"§c")+(weather?"":value>=0?"+":"")+number(value*(stat.percent()?100:1),weather?0:decimals)+(stat.percent()?"%":"")+"§f";
    }
    public static boolean adjusterBeneficial(ProtocolTankDesigner.Stat stat) {
        return java.util.Set.of("navy_fuel_consumption_factor","navy_visibility","naval_mine_hit_chance").contains(stat.id())?stat.value()<=0:stat.value()>=0;
    }
    public static String adjusterEffect(ProtocolTankDesigner.Stat stat) {
        int decimals=stat.id().equals("positioning")?1:2;
        return "§f"+tooltip("MODIFIER_"+(stat.id().equals("navy_visibility")?"NAVAL_VISIBILITY_FACTOR":stat.id().toUpperCase(java.util.Locale.ROOT)))+": "+(adjusterBeneficial(stat)?"§a":"§c")+(stat.value()>0?"+":"")+number(stat.value()*100,decimals)+"%§f";
    }
    public static String shipCategoryTooltip(String category) {
        String key="EQUIPMENT_DESIGNER_IS_"+category.toUpperCase(java.util.Locale.ROOT)+"_SHIP";
        return category.equals("submarine")?"잠수함":UiTooltipText.sections(tooltip(key+"_TOOLTIP"),tooltip(key+"_EFFECTS_TOOLTIP"),tooltip("EQUIPMENT_DESIGNER_VARIABLE_CATEGORY_TOOLTIP"));
    }
    private static String moduleModifier(String id,double value) {
        return (beneficial(id,value)?"§a":"§c")+(value>0?"+":"")+number(value*100,1)+"%§f";
    }
    public static String moduleEffects(String id) {
        var module=object(object(details(),"modules"),id);
        var added=object(module,"add").deepCopy();object(module,"average").entrySet().forEach(e->added.add(e.getKey(),e.getValue()));var multiplied=object(module,"multiply");
        var lines=new java.util.LinkedHashMap<String,String>();
        for(var stat:moduleStats(id)) {
            String key=stat.id();if(lines.containsKey(key))continue;
            String base=added.has(key)?"§e"+number(added.get(key).getAsDouble()*(java.util.Set.of("reliability","hardness").contains(key)?100:1),java.util.Set.of("fuel_consumption","build_cost_ic").contains(key)?2:1)+(java.util.Set.of("reliability","hardness").contains(key)?"%":""):"";
            String modifier=multiplied.has(key)?moduleModifier(key,multiplied.get(key).getAsDouble()):"";
            if(key.endsWith("_factor")&&added.has(key))base=moduleModifier(key,added.get(key).getAsDouble());
            String value=base+(base.isEmpty()||modifier.isEmpty()?"":", ")+modifier;
            if(object(module,"average").has(key))value+=averageLabel();
            String caption=statCaption(key,stat.name()).strip();
            if(key.equals("build_cost_ic")&&module.has("branch")&&module.get("branch").getAsString().equals("NAVY"))caption="£navprod_cost "+statName(key);
            lines.put(key,"§f"+caption+" "+value+"§f");
        }
        String cost=lines.remove("build_cost_ic");
        String category=moduleCategory(id);
        String role=java.util.Set.of("ship_heavy_battery","ship_super_heavy_battery","ship_medium_battery").contains(category)?tooltip("EQUIPMENT_MODULE_ADD_CAPITAL_SHIP_CATEGORY"):"";
        return UiTooltipText.sections(role,String.join("\n",lines.values()),moduleMissions(module),cost);
    }
    public static String statTooltip(ProtocolTankDesigner.Stat stat) {
        var cached=STAT_TOOLTIPS.get(stat);
        if(cached!=null)return cached;
        if(STAT_TOOLTIPS.size()>=64)STAT_TOOLTIPS.clear();
        String result=buildStatTooltip(stat);
        STAT_TOOLTIPS.put(stat,result);
        return result;
    }
    private static String buildStatTooltip(ProtocolTankDesigner.Stat stat) {
        if(stat.id().equals("manpower"))return "§f"+statCaption(stat.id(),stat.name()).strip().replaceAll(":$","");
        String title=stat.id().equals("build_cost_ic")?tooltip("EQUIPMENT_DESIGNER_PRODUCTION_COST_DESC"):
                "§e"+statCaption(stat.id(),stat.name()).strip().replaceAll(":$","")+"§f\n"+statDescription(stat.id());
        String sources=stat.contributions().stream().map(source->"§f"+source.name()+": "+signedValue(stat,source.value())).collect(java.util.stream.Collectors.joining("\n"));
        if(stat.id().equals("hit_profile"))sources=stat.contributions().stream().map(source->"§e"+statCaption(source.name(),statName(source.name()))+" §f"+number(source.value()*100,1)+"%").collect(java.util.stream.Collectors.joining("\n"));
        String changed=Math.abs(stat.change())<1e-8||stat.id().equals("build_cost_ic")?"":
                "§f"+tooltip("DESIGNER_UNSAVED_CHANGES")+" "+signedValue(stat,stat.change())
                +(beneficial(stat.id(),stat.change())?"§a":"§c")+(stat.change()>0?"▲":"▼");
        return UiTooltipText.sections(title,sources,changed);
    }
    public static String moduleDescription(String id) {
        var module=object(object(details(),"modules"),id);
        return sourceText(module.has("description")?module.get("description").getAsString():"");
    }
    public static java.util.List<ProtocolTankDesigner.Stat> moduleStats(String id) {
        return MODULE_STATS.computeIfAbsent(id,UiEquipmentDesigns::readModuleStats);
    }
    private static java.util.List<ProtocolTankDesigner.Stat> readModuleStats(String id) {
        var module=object(object(details(),"modules"),id);
        var result=new java.util.ArrayList<ProtocolTankDesigner.Stat>();
        for(String group:java.util.List.of("add","average","multiply"))for(var entry:object(module,group).entrySet()) {
            double value=entry.getValue().getAsDouble();
            if(value!=0)result.add(new ProtocolTankDesigner.Stat(entry.getKey(),statName(entry.getKey()),value,group.equals("multiply")||java.util.Set.of("reliability","hardness").contains(entry.getKey())));
        }
        var order=List.of("surface_visibility","surface_detection","sub_detection","lg_attack","lg_armor_piercing","hg_attack","hg_armor_piercing","torpedo_attack","sub_attack","anti_air_attack","naval_speed","naval_armor","max_strength","naval_light_gun_hit_chance_factor","naval_heavy_gun_hit_chance_factor","soft_attack","hard_attack","air_attack","defense","breakthrough","maximum_speed","armor_value","ap_attack","hardness","reliability","fuel_consumption","build_cost_ic");
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
    public static void overlays(GuiGraphicsExtractor g,ProtocolTankDesigner design,String hovered,int x,int y,int width,int height) {
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
