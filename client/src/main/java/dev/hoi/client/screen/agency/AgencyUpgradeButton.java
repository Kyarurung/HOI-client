package dev.hoi.client.screen.agency;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiPanelButton;
import dev.hoi.client.ui.UiText;
import dev.hoi.protocol.agency.AgencyView.Item;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class AgencyUpgradeButton extends UiPanelButton {
    private final Item item;
    AgencyUpgradeButton(Item item, int x, int y, int width, int height, Runnable action) {
        super(item.title(), x, y, width, height, action);
        this.item = item;
        int category = AgencyScreen.upgradeCategory(item);
        sound("ui.agency." + (category == 0 ? "intel" : category == 1 ? "defense" : category == 2 ? "operations" : category == 3 ? "training" : "cryptology"));
    }
    Item item() { return item; }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
        if (item.enabled()) super.playDownSound(manager);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth() - 13;
        var font = Minecraft.getInstance().font;
        String state = item.progress() >= 1 ? "completed" : item.enabled() ? "available" : "unavailable";
        int category = AgencyScreen.upgradeCategory(item);
        String shape = item.texture().startsWith("agency/unique_") ? "star" : category == 0 ? "diamond" : category == 1 ? "triangle" : category == 2 ? "octagon" : category == 3 ? "circle" : "star";
        UiAssets.draw(g, "agency/frames/" + shape + "_" + (state.equals("completed") ? "completed" : "unavailable"), x, y + 3, w, 35);
        UiAssets.draw(g, item.texture(), x, y + 3, w, 35);
        UiAssets.cover(g, "agency/ui/" + state, x, y + 31, w, 20);
        UiText.centered(g, font, item.title(), x + 2, y + 33, w - 4, 20, 0xFFE0E3DD);
        if (item.factories() > 0) {
            UiAssets.draw(g, "construction/summary/civilian_factory", x + w, y + 33, 13, 8);
            UiText.centered(g, font, Integer.toString(item.factories()), x + w, y + 41, 13, 10, Boolean.FALSE.equals(item.factoriesAvailable()) ? 0xFFFF3232 : 0xFFE0E3DD);
        }
        int stages = AgencyScreen.upgradeStages(item), level = AgencyScreen.upgradeLevel(item);
        for (int i = 0; stages > 1 && i < stages; i++)
            UiAssets.draw(g, i < level ? "agency/stage_complete" : "agency/stage_empty", x + (w - stages * 6) / 2 + i * 6, y + 48, 8, 8);
    }
}
