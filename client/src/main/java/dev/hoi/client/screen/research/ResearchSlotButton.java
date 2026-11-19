package dev.hoi.client.screen.research;

import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.research.ResearchView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ResearchSlotButton extends Button {
    private final ResearchView.Slot slot;
    private final ResearchView.Tech technology;
    private final String country;
    private final float left, top, scale;
    ResearchSlotButton(ResearchView.Slot slot, ResearchView.Tech technology, String country, float x, float y, float scale, Runnable action) {
        super(Math.round(x), Math.round(y), Math.round(508 * scale), Math.round(99 * scale), Component.literal(technology == null ? dev.hoi.client.ui.UiCommandLabels.text("RESEARCH_NO_RESEARCH", "사용되지 않음") : technology.name()),
                b -> action.run(), DEFAULT_NARRATION);
        this.slot = slot; this.technology = technology; this.country = country; this.left = x; this.top = y; this.scale = scale;
    }
    public int index() { return slot.index(); }
    float sourceScale() { return scale; }
    float sourceLeft() { return left; }
    float sourceTop() { return top; }
    int sourceX(int x) { return Math.round(left + x * scale); }
    int sourceY(int y) { return Math.round(top + y * scale); }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { if (technology == null) AudioUiSounds.play("ui.research.select"); }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;
        String background = technology != null && !slot.designTeam().isEmpty() ? "mio/ui/gfx_research_line_mio_bg/frame_0" : "panel/research_slot";
        g.pose().pushMatrix();
        g.pose().translate(left, top);
        g.pose().scale(scale);
        UiAssets.draw(g, background, 0, 0, 508, 99);
        if (technology == null) {
            long phase = dev.hoi.client.ui.UiText.marqueeMillis() % 2000;
            int alpha = Math.round(255 * (phase < 1000 ? phase : 2000 - phase) / 1000f);
            if (alpha > 0) UiAssets.drawTinted(g, "research/ui/empty_slot_glow/frame_1", 5, 5, 494, 83, alpha << 24 | 0xFFFFFF);
        }
        if (technology != null) {
            g.blit(texture(background), 220, 16, 284, 76, 300 / 508f, 364 / 508f, 16 / 99f, 76 / 99f);
            String art = UiAssets.technologyPath(country, technology);
            int[] size = UiAssets.imageSize(art);
            if (size[0] > 0 && size[1] > 0) {
                g.pose().pushMatrix();
                g.pose().translate(252 - size[0] / 2f, 49 - size[1] / 2f);
                UiAssets.draw(g, art, 0, 0, size[0], size[1]);
                g.pose().popMatrix();
            } else UiAssets.technology(g, country, technology, 217, 17, 70, 64, 0xFFFFFFFF);
        }
        String progress = "research/slot/gfx_research_progressbar";
        UiAssets.draw(g, progress + "_frame/frame_0", 62, 79, 383, 10);
        UiAssets.draw(g, progress + "_background/frame_0", 65, 81, 377, 4);
        float fraction = (float)(technology == null ? slot.savedDays() / 30.0 : technology.fraction());
        if (fraction > 0) {
            int right = 65 + Math.round(377 * fraction);
            var fill = texture(progress + "_fill/frame_0");
            g.blit(fill, 65, 81, right, 85, 0, fraction, 1 / 3f, 2 / 3f);
            g.blit(fill, 65, 81, right, 85, 0, fraction, 0, 1 / 3f);
            g.blit(fill, 65, 81, right, 85, 1 - fraction, 1, 2 / 3f, 1);
        }
        g.pose().popMatrix();
        g.nextStratum();
        String title = technology == null ? "연구할 기술을 선택하세요." : technology.name();
        float titleScale = 1f;
        int titleWidth = Math.round(400 * scale / titleScale);
        boolean overflow = font.width(title) > titleWidth;
        int offset = dev.hoi.client.ui.UiText.scrollingOffset(Math.max(0, font.width(title) - titleWidth), dev.hoi.client.ui.UiText.marqueeMillis());
        g.pose().pushMatrix();
        g.pose().translate(sourceX(57), sourceY(12));
        g.pose().scale(titleScale);
        if (overflow) g.enableScissor(0, -1, titleWidth, font.lineHeight + 1);
        float outline = 1f / Minecraft.getInstance().getWindow().getGuiScale();
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) continue;
                g.pose().pushMatrix();
                g.pose().translate(x * outline, y * outline);
                g.text(font, title, -offset, 0, 0xFF000000, false);
                g.pose().popMatrix();
            }
        }
        g.nextStratum();
        g.text(font, title, -offset, 0, 0xFFE0E6E8, false);
        if (overflow) g.disableScissor();
        g.pose().popMatrix();
        if (technology != null) {
            var status = Component.literal(Long.toString(technology.remainingDays(slot.savedDays()))).withStyle(style -> style.withColor(0xFFD060))
                    .append(Component.literal("일").withStyle(style -> style.withColor(0xFFFFFF)));
            g.text(font, status, sourceX(460) - font.width(status), sourceY(12), 0xFFFFFFFF);
            g.text(font, Integer.toString(technology.year()), sourceX(57), Math.min(sourceY(60), sourceY(79) - font.lineHeight), 0xFFE0E6E8);
        }
    }
    private static Identifier texture(String path) { return Identifier.fromNamespaceAndPath("hoi", "textures/gui/" + path + ".png"); }
}
