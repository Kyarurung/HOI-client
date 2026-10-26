package dev.hoi.client.research;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.ProtocolResearchView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ResearchSlotButton extends Button {
    private final ProtocolResearchView.Slot slot;
    private final ProtocolResearchView.Tech technology;
    private final String country;
    ResearchSlotButton(ProtocolResearchView.Slot slot, ProtocolResearchView.Tech technology, String country, int x, int y, int width, int height, Runnable action) {
        super(x, y, width, height, Component.literal("슬롯 " + (slot.index() + 1) + " · " + (technology == null ? "연구 선택" : technology.name())),
                b -> action.run(), DEFAULT_NARRATION);
        this.slot = slot; this.technology = technology; this.country = country;
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { if (technology == null) AudioUiSounds.play("ui.research.select"); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        var font = Minecraft.getInstance().font;
        int color = 0xFF94B4C4;
        g.fillGradient(x, y, x + w, y + h, 0xFF3E5A70, 0xFF172731);
        String background=slot.designTeam().isEmpty()?"panel/research_slot":"mio/ui/gfx_research_line_mio_bg/frame_0";
        UiAssets.draw(g,background,x,y,w,h);
        if(technology!=null){var image=net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi","textures/gui/"+background+".png");g.blit(image,x+Math.round(w*.44f),y+Math.round(h*.15f),x+Math.round(w*.56f),y+Math.round(h*.76f),.59f,.71f,.15f,.76f);}
        g.outline(x, y, w, h, color);
        String title = technology == null ? "연구할 기술을 선택하세요." : technology.name();
        int titleWidth = technology == null ? w - 20 : w - 65;
        if (font.width(title) > titleWidth) title = font.plainSubstrByWidth(title, Math.max(1, titleWidth - 9)) + "…";
        int titleY = 6;
        if (technology != null) {
            int imageWidth = 35;
            int imageHeight = 22;
            String art = UiAssets.technologyPath(country, technology);
            if (!UiAssets.drawContent(g, art, x + (w - imageWidth) / 2, y + 15, imageWidth, imageHeight))
                UiAssets.technology(g, country, technology, x + (w - imageWidth) / 2, y + 15,
                    imageWidth, imageHeight, color);
            String status = technology.remainingDays(slot.savedDays()) + "일";
            int statusX = x + w - 10 - font.width(status);
            g.text(font, Component.literal(Long.toString(technology.remainingDays(slot.savedDays()))).withStyle(style->style.withColor(0xFFD060)).append(Component.literal("일").withStyle(style->style.withColor(0xFFFFFF))), statusX, y + titleY, color);
            g.text(font, Integer.toString(technology.year()), x + 10, y + h - 15, 0xFFE0E6E8);
        }
        g.text(font, title, x + 10, y + titleY, 0xFFE0E6E8);
        if (h >= 20) {
            g.fill(x + 4, y + h - 5, x + w - 4, y + h - 2, 0xFF0C1217);
            if (technology != null) g.fill(x + 4, y + h - 5, x + 4 + (int)((w - 8) * technology.fraction()), y + h - 2, color);
        }
    }
}
