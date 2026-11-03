package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.protocol.ProtocolAgencyView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class MapAgencyOperations {
    public record Hit(int x, int y, String item, String tooltip) {
        boolean contains(double px, double py) { return px >= x - 10 && px < x + 34 && py >= y - 10 && py < y + 34; }
    }
    private List<Hit> hits = List.of();
    public List<Hit> hits() { return hits; }
    public void clear() { hits = List.of(); }
    public void draw(GuiGraphicsExtractor g, ProtocolAgencyView view, int left, int top) {
        clear();
        var client = Minecraft.getInstance();
        if (client.level == null || view == null || !client.level.dimension().identifier().toString().startsWith("hoi:atlas_")) return;
        var camera = client.gameRenderer.mainCamera();
        var projection = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());
        var forward = camera.forwardVector();
        var result = new ArrayList<Hit>();
        for (var operation : view.operations()) {
            var item = view.items().stream().filter(value -> value.id().equals(operation.item())).findFirst().orElse(null);
            if (item == null || operation.mapIcon().isEmpty() || operation.province() == 0) continue;
            int blockX = (int)Math.floor(operation.x()), blockZ = (int)Math.floor(operation.z());
            if (!client.level.hasChunk(blockX >> 4, blockZ >> 4)) continue;
            double ground = client.level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            var relative = new Vec3(operation.x(), ground + 1, operation.z()).subtract(camera.position());
            if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= 0) continue;
            var point = projection.transformProject(relative.toVector3f());
            if (!point.isFinite() || Math.abs(point.x) > 1 || Math.abs(point.y) > 1) continue;
            int x = Math.round((point.x + 1) * g.guiWidth() / 2), y = Math.round((1 - point.y) * g.guiHeight() / 2) - 12;
            if (x - 10 < left || x + 34 >= g.guiWidth() || y - 10 < top || y + 34 >= g.guiHeight()) continue;
            UiAssets.draw(g, "agency/operations/" + operation.mapIcon().toLowerCase(Locale.ROOT) + "/frame_0", x - 10, y - 10, 24, 24);
            if (operation.progress() != null) {
                dev.hoi.client.ui.UiAgencyProgress.draw(g, x, y - 4, operation.progress().fraction());
                var phase = operation.phases().get(operation.progress().index());
                if (!phase.icon().isEmpty()) UiAssets.draw(g, "agency/operations/" + phase.icon().toLowerCase(Locale.ROOT) + "/frame_0", x + 2, y - 2, 21, 21);
            }
            UiAssets.draw(g, "agency/map/cartouche/frame_0", x - 8, y + 20, 42, 14);
            UiText.centered(g, client.font, operation.stage().equals("COMPLETED") ? item.button() : (operation.total() - operation.days()) + "일", x - 7, y + 22, 40, 10, 0xFFE7C779);
            String detail = "§Y" + operation.name() + "§!";
            if (!operation.completes().isBlank()) detail += "\n작전이 §H" + operation.completes() + "§!에 완료될 것입니다";
            detail += "\n\n세부사항을 보려면 §G클릭§!.";
            if (!operation.detail().isEmpty()) detail = operation.detail();
            result.add(new Hit(x, y, item.id(), detail));
        }
        hits = List.copyOf(result);
    }
    public boolean click(double x, double y, int button, Consumer<String> open) {
        var hit = hits.reversed().stream().filter(value -> value.contains(x, y)).findFirst().orElse(null);
        if (hit == null) return false;
        if (button == InputConstants.MOUSE_BUTTON_LEFT) { clear(); dev.hoi.client.audio.AudioUiSounds.play("ui.click"); open.accept(hit.item()); }
        return true;
    }
    public void tooltip(GuiGraphicsExtractor g, int x, int y) {
        hits.reversed().stream().filter(hit -> hit.contains(x, y)).findFirst()
            .ifPresent(hit -> g.setTooltipForNextFrame(Minecraft.getInstance().font,
                UiTooltips.sourceLines(Minecraft.getInstance().font, hit.tooltip(), Math.min(g.guiWidth(), 244)), x, y));
    }
}
