package dev.hoi.client.map.supply;

import dev.hoi.client.ResearchScreenTest;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.dialog.DialogPayloads;
import dev.hoi.protocol.map.MapSupplyHover;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class SupplyHubHoverTest {
    static final String TEXT = "§e청더§r의 §e보급 중심지§r:\n\n보급 수요: §e0.1§r £supply_texticon£\n  사단: §e9§r (§e0.10§r)\n"
            + "§e60§r 사용 중 / §e60§r 트럭 £unit_motorized_icon_small£\n보급 사령부로부터의 보급 연결: §e15.0§r £supply_texticon£\n"
            + "  열차 필요: §e1§r/§e1§r £GFX_infrastructure_texticon£(§e100%§r)\n§e강 병목:§r\n    §e친황다오§r - §e청더§r\n  강으로부터: §c15§r";
    static final String DELAYED = "§e보급 중심지§r는 보급품을 주변 지역으로 분배합니다. 보급 범위 및 효율은 날씨, 지형, 기반시설, 철도 레벨, 차량화에 따라 결정됩니다.\n\n"
            + "§e보급 중심지§r의 보급 범위 및 보급량을 늘리려면 §e철도 증설§r을 사용하거나, §e차량화 레벨§r을 변경하거나, §e기반시설 레벨§r을 올리십시오.\n\n"
            + "§aShift§r를 길게 누르면 범위가 표시됩니다.\n§aShift-클릭§r하여 차량화를 전환하십시오.\n§aCtrl-클릭§r하여 동맹 보급을 전환하십시오.";
    private SupplyHubHoverTest() {}
    private static final class SupplyHubHoverPreview extends Screen {
        SupplyHubHoverPreview() { super(Component.literal("보급 중심지")); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(0, 0, width, height, 0xFF2F4A3A);
            SupplyHubHover.draw(graphics, font, 40, 30, width, height);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
    public static void run(ClientGameTestContext context) {
        var requests = new ArrayList<MapSupplyHover.Request>();
        context.runOnClient(client -> {
            SupplyHubHover.reset(); SupplyHubHover.queryObserver = requests::add; SupplyHubHover.point(0, -1, 0); SupplyHubHover.tick();
            if (requests.size() != 1) throw new AssertionError("The first tick sends the cursor ray");
            SupplyHubHover.tick();
            if (requests.size() != 1) throw new AssertionError("Hover requests wait three ticks for a reply");
        });
        long[] shown = new long[1];
        context.runOnClient(client -> {
            var request = requests.getLast();
            if (request.x() != 0 || request.y() != -1 || request.z() != 0) throw new AssertionError("The hover request carries the cursor ray");
            SupplyHubHover.accept(new MapSupplyHover.Tooltip(request.sequence() + 1, "hub/1", "stale", ""));
            if (!SupplyHubHover.text().isEmpty()) throw new AssertionError("Replies for another request are ignored");
            SupplyHubHover.accept(new MapSupplyHover.Tooltip(request.sequence(), "hub/1", TEXT, DELAYED));
            shown[0] = Util.getMillis();
            if (!SupplyHubHover.text().equals(TEXT)) throw new AssertionError("The hub tooltip shows the immediate part first");
        });
        context.waitFor(client -> Util.getMillis() - shown[0] > SupplyHubHover.DELAY_MILLIS + 100);
        context.runOnClient(client -> {
            if (!SupplyHubHover.text().equals(TEXT + "\n\n" + DELAYED)) throw new AssertionError("The delayed description follows one blank line");
            var lines = Tooltips.sourceLines(client.font, SupplyHubHover.text(), client.getWindow().getGuiScaledWidth());
            int blanks = 0; boolean previousBlank = false;
            for (var line : lines) {
                var plain = new StringBuilder(); line.accept((index, style, codePoint) -> { plain.appendCodePoint(codePoint); return true; });
                String value = plain.toString();
                if (value.contains("£") || value.contains("$") || value.contains("§") || value.contains("---")) throw new AssertionError("Raw token in supply tooltip: " + value);
                boolean blank = value.isBlank();
                if (blank && previousBlank) throw new AssertionError("Repeated blank tooltip lines");
                if (blank) blanks++; previousBlank = blank;
            }
            if (blanks != 4) throw new AssertionError("Header, separator and description blank lines: " + blanks);
        });
        context.setScreen(SupplyHubHoverPreview::new);
        context.waitTicks(2);
        ResearchScreenTest.gui2Screenshot(context, "hoi-supply-hub-tooltip");
        context.runOnClient(client -> {
            SupplyHubHover.accept(new MapSupplyHover.Tooltip(requests.getLast().sequence(), "", "", ""));
            if (!SupplyHubHover.text().isEmpty() || !SupplyHubHover.target().isEmpty()) throw new AssertionError("Leaving the hub clears the tooltip");
            SupplyHubHover.queryObserver = null; SupplyHubHover.reset();
        });
        var click = new DialogPayloads.MapClick(0, -1, 0, DialogPayloads.MapClick.SHIFT | DialogPayloads.MapClick.CONTROL);
        if (click.modifiers() != 3 || new DialogPayloads.MapClick(0, -1, 0).modifiers() != 0) throw new AssertionError("Map clicks carry Shift and Ctrl");
        context.setScreen(() -> null);
    }
}
