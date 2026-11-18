package dev.hoi.client.ui.industry;

import dev.hoi.client.ui.*;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class IndustryOrganizationList {
    public static final int WIDTH = 275, HEIGHT = 334, LIST_TOP = 48, ENTRY_WIDTH = 260, ENTRY_HEIGHT = 50;
    private IndustryOrganizationList() {}
    public static void frame(GuiGraphicsExtractor g, Font font, int x, int y, int width, int height) {
        UiAssets.tiledNineSlice(g, IndustryOrganizationRow.art("tiled_bg"), x, y, width, height, 64, .5);
        UiAssets.tiledNineSlice(g, IndustryOrganizationRow.art("tiled_window_thin_border2"), x, y + 42, width, height - 44, 64, .5);
        UiText.centeredSingleLine(g, font, Tooltips.officerText(UiCommandLabels.text("INDUSTRIAL_ORG_LIST_WINDOW_TITLE", "군수산업체"), true), x + 8, y + 5, width - 21, 11, UiMenuStyle.TEXT);
    }
    public static void empty(GuiGraphicsExtractor g, Font font, String text, int x, int y) {
        UiAssets.draw(g, IndustryOrganizationRow.art("mio_entry_bg"), x, y, ENTRY_WIDTH, ENTRY_HEIGHT);
        UiText.centeredSingleLine(g, font, Tooltips.officerText(text), x + 5, y + 20, ENTRY_WIDTH - 10, 10, UiMenuStyle.TEXT);
    }
    public static UiMenuButton close(int x, int y, int width, Runnable action) {
        return new UiMenuButton("×", x + width - 19, y + 2, 16, 17, action).caption("").background(IndustryOrganizationRow.art("closebutton")).sound("ui.close");
    }
    public static UiMenuButton details(Font font, String organization, int x, int y) {
        var button = new UiMenuButton(UiCommandLabels.text("INDUSTRIAL_ORG_OPEN_DETAILS_WINDOW", "세부 사항"), x + 200, y + 25, 46, 16,
                () -> dev.hoi.client.map.commander.CommanderHud.open(new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.ORGANIZATION, organization, List.of())))
                .background(IndustryOrganizationRow.art("leader_selection_button"));
        button.textScale(1f - 1f / font.lineHeight);
        return button;
    }
}
