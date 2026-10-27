package dev.hoi.client.ui;

import dev.hoi.protocol.ProtocolTankDesigner;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UiTerrainAdjustment {
    private UiTerrainAdjustment() {}
    public static String tooltip(ProtocolTankDesigner.Adjustment value) {
        var lines = new ArrayList<String>();
        lines.add("§e" + UiEquipmentDesigns.adjusterName(value.id()));
        double[] values = {value.movement(), value.attack(), value.defense()};
        String[] keys = {"MOVEMENT", "ATTACK", "DEFENCE"};
        for (int i = 0; i < 3; i++) lines.add("§f" + UiEquipmentDesigns.tooltip("STAT_ADJUSTER_" + keys[i]).stripTrailing().replaceAll(":$", "") + ": " + (values[i] < 0 ? "§c" : "§a") + String.format(Locale.ROOT, "%+.1f%%", values[i] * 100));
        return String.join("\n\n", lines);
    }
    public static void draw(GuiGraphicsExtractor g, ProtocolTankDesigner.Adjustment value, int x, int y, IntUnaryOperator size) {
        UiAssets.draw(g, "production/design/adjusters/" + value.id(), x + size.applyAsInt(12), y + size.applyAsInt(5), size.applyAsInt(40), size.applyAsInt(40));
        UiAssets.draw(g, "production/design/controls/adjuster_frame", x, y, size.applyAsInt(64), size.applyAsInt(57));
        double[] values = {value.movement(), value.attack(), value.defense()};
        String[] icons = {"movement", "attack", "defense"};
        for (int i = 0; i < 3; i++) {
            String key = "production/design/controls/" + icons[i];
            UiAssets.drawAlignedContent(g, key + "_" + (values[i] > 1e-8 ? "positive" : values[i] < -1e-8 ? "negative" : "neutral"), key + "_neutral", x + size.applyAsInt(4 + i * 22), y + size.applyAsInt(38), size.applyAsInt(20), size.applyAsInt(20));
        }
    }
}
