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
    private UiEquipmentDesigns() {}
    public static void clear() {details=null;}
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
