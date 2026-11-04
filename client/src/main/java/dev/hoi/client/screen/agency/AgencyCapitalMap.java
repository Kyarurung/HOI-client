package dev.hoi.client.screen.agency;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.protocol.agency.AgencyView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AgencyCapitalMap {
    record Hit(int x, int y, String country, AgencyView.Item item, AgencyView.Capital capital, AgencyView.Cipher cipher) {
        boolean contains(double px, double py) { return px >= x - 9 && px < x + 36 && py >= y && py < y + 41; }
    }
    private List<Hit> hits = List.of();
    private String hovered = "";
    List<Hit> hits() { return hits; }
    void clear() { hits = List.of(); }
    void draw(GuiGraphicsExtractor g, AgencyView view, int left, int top) {
        clear();
        var client = Minecraft.getInstance();
        if (client.level == null || view == null || !client.level.dimension().identifier().toString().startsWith("hoi:atlas_")) return;
        var camera = client.gameRenderer.mainCamera();
        var matrix = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());
        var forward = camera.forwardVector();
        var visible = new ArrayList<Hit>();
        for (var capital : view.capitals()) {
            var item = view.items().stream().filter(value -> value.id().equals("decrypt:" + capital.country()) || value.id().equals("pause:" + capital.country())).findFirst().orElse(null);
            var cipher = view.ciphers().stream().filter(value -> value.country().equals(capital.country())).findFirst().orElse(null);
            if (item == null && capital.tooltip().isEmpty() && capital.item().isEmpty() && cipher == null) continue;
            int blockX = (int)Math.floor(capital.x()), blockZ = (int)Math.floor(capital.z());
            if (!client.level.hasChunk(blockX >> 4, blockZ >> 4)) continue;
            double ground = client.level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            var relative = new Vec3(capital.x(), ground + 1, capital.z()).subtract(camera.position());
            if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= 0) continue;
            var point = matrix.transformProject(relative.toVector3f());
            if (!point.isFinite() || Math.abs(point.x) > 1 || Math.abs(point.y) > 1) continue;
            int x = Math.round((point.x + 1) * g.guiWidth() / 2), y = Math.round((1 - point.y) * g.guiHeight() / 2) - 20;
            if (x - 9 < left || x + 36 >= g.guiWidth() || y < top || y + 41 >= g.guiHeight()) continue;
            var hit = new Hit(x, y, capital.country(), item, capital, cipher);
            visible.add(hit);
            drawIcon(g, hit);
        }
        hits = List.copyOf(visible);
    }
    private void drawIcon(GuiGraphicsExtractor g, Hit hit) {
        int x = hit.x(), y = hit.y();
        var item = hit.item();
        var cipher = hit.cipher();
        double progress = cipher != null ? cipher.progress() : item != null ? item.progress() : -1;
        boolean decrypting = cipher != null ? cipher.state().equals("decrypting") : item != null && item.id().startsWith("pause:");
        dev.hoi.client.ui.UiCountryFlags.draw(g,"country/" + hit.country().toLowerCase(Locale.ROOT) + "/flag","small2", x - 7, y + 1, 15, 10);
        UiAssets.draw(g, "agency/map/flag_frame/frame_0", x - 9, y, 17, 11);
        if (progress >= 0) dev.hoi.client.ui.UiAgencyProgress.draw(g, x + 3, y + 3, progress);
        String icon = progress >= 1 ? "done/frame_0" : decrypting
                ? "animated/frame_" + (int)((net.minecraft.util.Util.getMillis() * 24 / 1000) % 73) : "still/frame_0";
        UiAssets.draw(g, "agency/map/" + icon, x + 6, y + 5, 20, 21);
        if (progress >= 0) {
            UiAssets.draw(g, "agency/map/cartouche/frame_0", x - 6, y + 26, 42, 14);
            UiText.centered(g, Minecraft.getInstance().font, cipher != null ? cipher.status() : String.format(Locale.ROOT, "%.0f%%", progress * 100), x - 7, y + 29, 47, 10, 0xFFE0E3DD);
        }
    }
    Hit at(double x, double y) {
        return hits.reversed().stream().filter(hit -> hit.contains(x, y)).findFirst().orElse(null);
    }
    void tooltip(GuiGraphicsExtractor g, int x, int y) {
        var hit = at(x, y);
        String country = hit == null ? "" : hit.country();
        if (!country.equals(hovered)) {
            hovered = country;
            if (!country.isEmpty()) dev.hoi.client.audio.AudioUiSounds.play("ui.agency.decrypt.hover");
        }
        if (hit != null) UiTooltips.drawExact(g, Minecraft.getInstance().font, hit.cipher() != null ? hit.cipher().activateTooltip() : !hit.capital().tooltip().isEmpty() ? hit.capital().tooltip() : hit.item().detail(), x, y, 175);
    }
}
