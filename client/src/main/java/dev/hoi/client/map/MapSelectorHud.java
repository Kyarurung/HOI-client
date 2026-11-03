package dev.hoi.client.map;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiText;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MapSelectorHud {
    record Entry(int slot,ItemStack item) {}
    record Bounds(int x,int y,int width,int height) {}
    private MapSelectorHud() {}
    static boolean mapDimension(String dimension){return dimension.startsWith("hoi:atlas_");}
    static boolean active(){var level=Minecraft.getInstance().level;return level!=null&&mapDimension(level.dimension().identifier().toString());}
    static boolean selector(ItemStack item){var model=item.get(DataComponents.ITEM_MODEL);return item.is(Items.CARROT_ON_A_STICK)&&model!=null&&model.getNamespace().equals("hoi")&&model.getPath().startsWith("map/selector/");}
    static boolean hidesMessages(){return active()||MapCommanderHud.hidesVanillaHud();}
    public static void register(){
        for(var id:List.of(VanillaHudElements.HOTBAR,VanillaHudElements.SPECTATOR_MENU,VanillaHudElements.ARMOR_BAR,VanillaHudElements.HEALTH_BAR,VanillaHudElements.FOOD_BAR,VanillaHudElements.AIR_BAR,VanillaHudElements.MOUNT_HEALTH,VanillaHudElements.INFO_BAR,VanillaHudElements.EXPERIENCE_LEVEL))HudElementRegistry.replaceElement(id,original->(g,delta)->{if(!MapCommanderHud.hidesVanillaHud())original.extractRenderState(g,delta);});
        for(var id:List.of(VanillaHudElements.HELD_ITEM_TOOLTIP,VanillaHudElements.SPECTATOR_TOOLTIP,VanillaHudElements.OVERLAY_MESSAGE))HudElementRegistry.replaceElement(id,original->(g,delta)->{if(!hidesMessages())original.extractRenderState(g,delta);});
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,Identifier.parse("hoi:map_selector_legend"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)draw(g);});
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{if(screen instanceof InputSidebarMovement.InputMovementScreen sidebar)ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)->{if(sidebar.allowsMovement())draw(g);});});
    }
    private static void draw(GuiGraphicsExtractor g){
        var client=Minecraft.getInstance();if(!active()||client.player==null||client.gui.hud.isHidden())return;var entries=new ArrayList<Entry>();for(int slot=0;slot<9;slot++){var item=client.player.getInventory().getItem(slot);if(selector(item))entries.add(new Entry(slot,item));}draw(g,entries);
    }
    static Bounds bounds(int width,int height,List<Entry> entries){
        var font=Minecraft.getInstance().font;int textWidth=entries.stream().mapToInt(e->font.width(e.item().getHoverName())+44).max().orElse(0),rows=entries.size()*18;
        int x=Math.max(3,width-textWidth-6),bottom=height-6;
        for(var box:MapCommanderHud.boxes())if(x<box.x()+box.width()&&x+textWidth>box.x()&&box.y()+box.height()>bottom-rows)bottom=Math.min(bottom,box.y()-6);
        return new Bounds(x,Math.max(UiMenuBar.height(width)+8,bottom-rows),textWidth,rows);
    }
    static void draw(GuiGraphicsExtractor g,List<Entry> entries){
        var font=Minecraft.getInstance().font;var bounds=bounds(g.guiWidth(),g.guiHeight(),entries);int row=18,key=16,icon=16,x=bounds.x(),y=bounds.y();
        for(var entry:entries){
            UiAssets.nineSlice(g,"commander/commands/nv_btn_select/frame_0",x,y+2,key,key,4,.5);
            UiText.centeredSingleLine(g,font,net.minecraft.network.chat.Component.literal(Integer.toString(entry.slot()+1)),x,y+2,key,key,0xffe0e0e0);
            g.item(entry.item(),x+key+4,y+2);
            UiText.text(g,font,entry.item().getHoverName(),x+key+icon+8,y+(row-font.lineHeight)/2,0xffffffff);
            y+=row;
        }
    }
}
