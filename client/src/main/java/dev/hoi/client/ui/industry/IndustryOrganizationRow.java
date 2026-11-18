package dev.hoi.client.ui.industry;

import dev.hoi.protocol.menu.MenuView;
import dev.hoi.client.ui.*;
import dev.hoi.client.ui.tooltip.TooltipKeywordIcons;
import dev.hoi.client.ui.tooltip.Tooltips;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class IndustryOrganizationRow {
    private IndustryOrganizationRow() {}
    public static String art(String name) { return "mio/ui/gfx_" + name + "/frame_0"; }
    public static String equipment(String token) { return art(token.startsWith("mio_cat_") ? token : "military_industrial_organization_" + token); }
    public static void draw(GuiGraphicsExtractor g, String logo, int size, String name, String type,
            List<MenuView.ManufacturerEquipment> equipment, int x, int y, int width) {
        draw(g,logo,size,name,type,equipment,x,y,width,"");
    }
    public static void draw(GuiGraphicsExtractor g,String logo,int size,String name,String type,List<MenuView.ManufacturerEquipment> equipment,int x,int y,int width,String cost){
        var font = Minecraft.getInstance().font;
        UiAssets.nineSlice(g, art("mio_entry_bg"), x, y, width, 50, 8, .5);
        int[] logoSize = UiAssets.imageSize(logo);
        UiAssets.draw(g, logo, x + 5, y + 10, (logoSize[0] + 1) / 2, (logoSize[1] + 1) / 2);
        UiAssets.draw(g, art("land_battle_leader_skill_bg"), x + 40, y + 10, 18, 25);
        UiText.centeredSingleLine(g, font, Tooltips.officerText(Integer.toString(size), true), x + 40, y + 14, 17, 9, UiMenuStyle.TEXT);
        UiText.leftSingleLine(g, font, net.minecraft.network.chat.Component.literal(name), x + 63, y + 6, Math.min(130, width - 70), 10, UiMenuStyle.TEXT);
        UiText.leftSingleLine(g, font, net.minecraft.network.chat.Component.literal(type), x + 63, y + 16, Math.min(130, width - 70), 10, 0xFFFFD060);
        if (!cost.isEmpty()) {
            var value = TooltipKeywordIcons.decorateLines(Tooltips.officerText(cost), false);
            int costWidth = Math.min(45, font.width(value));
            UiText.leftSingleLine(g, font, value, x + Math.min(233, width - 4) - costWidth, y + 7, costWidth, 10, UiMenuStyle.TEXT);
        }
        int count = equipment.size(), pitch = count * 35 <= 175 ? 35 : 140 / (count - 1);
        for (int i = 0; i < count; i++) {
            var item = equipment.get(i); int ex = x + 60 + i * pitch;
            UiAssets.draw(g, equipment(item.token()), ex, y + 25, 19, 19);
            UiText.leftSingleLine(g, font, net.minecraft.network.chat.Component.literal(Integer.toString(item.traits())), ex + 21, y + 33, 12, 10, UiMenuStyle.TEXT);
        }
    }
}
