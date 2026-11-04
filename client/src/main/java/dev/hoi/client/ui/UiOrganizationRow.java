package dev.hoi.client.ui;

import dev.hoi.protocol.menu.MenuView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UiOrganizationRow {
    private UiOrganizationRow() {}
    public static String art(String name) { return "mio/ui/gfx_" + name + "/frame_0"; }
    public static String equipment(String token) { return art(token.startsWith("mio_cat_") ? token : "military_industrial_organization_" + token); }
    public static void draw(GuiGraphicsExtractor g, String logo, int size, String name, String type,
            List<MenuView.ManufacturerEquipment> equipment, int x, int y, int width) {
        draw(g,logo,size,name,type,equipment,x,y,width,"");
    }
    public static void draw(GuiGraphicsExtractor g,String logo,int size,String name,String type,List<MenuView.ManufacturerEquipment> equipment,int x,int y,int width,String cost){
        var font = Minecraft.getInstance().font;
        UiAssets.nineSlice(g, art("mio_entry_bg"), x, y, width, 49, 8, .5);
        UiAssets.drawContent(g, logo, x + 3, y + 7, 33, 35);
        UiAssets.draw(g, art("land_battle_leader_skill_bg"), x + 38, y + 6, 17, 25);
        UiText.centered(g, font, Integer.toString(size), x + 38, y + 7, 17, 16, UiMenuStyle.TEXT, false);
        UiText.leftSingleLine(g, font, net.minecraft.network.chat.Component.literal(name), x + 60, y + 3, width - (cost.isEmpty()?65:137), 14, UiMenuStyle.TEXT);
        UiText.leftSingleLine(g, font, net.minecraft.network.chat.Component.literal(type), x + 60, y + 17, width - 65, 13, 0xFFFFD060);
        if(!cost.isEmpty())UiText.leftSingleLine(g,font,UiKeywordIcons.decorateLines(UiTooltips.officerText(cost),false),x+width-72,y+3,68,14,UiMenuStyle.TEXT);
        int count = Math.min(5, equipment.size()), pitch = Math.min(27, Math.max(20, (width - 118) / Math.max(1, count)));
        for (int i = 0; i < count; i++) {
            var item = equipment.get(i); int ex = x + 60 + i * pitch;
            UiAssets.draw(g, equipment(item.token()), ex, y + 31, 18, 16);
            UiText.leftSingleLine(g, font, net.minecraft.network.chat.Component.literal(Integer.toString(item.traits())), ex + 17, y + 36, 12, 11, UiMenuStyle.TEXT);
        }
    }
}
